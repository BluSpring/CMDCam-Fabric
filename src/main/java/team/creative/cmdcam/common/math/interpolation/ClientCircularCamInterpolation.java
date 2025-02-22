package team.creative.cmdcam.common.math.interpolation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.cmdcam.common.utils.EnvExecutor;
import team.creative.creativecore.common.util.math.interpolation.HermiteInterpolation;
import team.creative.creativecore.common.util.math.interpolation.Interpolation;
import team.creative.creativecore.common.util.math.vec.Vec1d;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.math.vec.VecNd;

import java.util.ArrayList;
import java.util.List;


public class ClientCircularCamInterpolation extends CircularCamInterpolation{
    public ClientCircularCamInterpolation(boolean clockwise) {
        super(clockwise);
    }

    @Environment(EnvType.CLIENT)
    @OnlyIn(Dist.CLIENT)
    public <T extends VecNd> Interpolation<T> createClient(double[] timed, CamScene scene, T before, List<T> points, T after, CamAttribute<T> attribute) {
        return EnvExecutor.safeCallWhenOn(EnvType.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            Vec3d center = scene.lookTarget.position(mc.level, mc.getDeltaFrameTime());
            if (center != null) {
                List<Vec3d> points3 = (List<Vec3d>) points;
                points.add(points.get(0));
                Vec3d firstPoint = new Vec3d(points3.get(0).x, points3.get(0).y, points3.get(0).z);
                Vec3d centerPoint = new Vec3d(center.x, center.y, center.z);
                Vec3d sphereOrigin = new Vec3d(firstPoint);
                sphereOrigin.sub(centerPoint);

                double radius = sphereOrigin.length();

                ArrayList<Vec1d> vecs = new ArrayList<>();
                ArrayList<Double> times = new ArrayList<>();

                times.add(0D);
                vecs.add(new Vec1d(firstPoint.y));

                ArrayList<Vec3d> newPointsSorted = new ArrayList<>();
                newPointsSorted.add(points3.get(0));

                for (int i = 1; i < points.size() - 1; i++) {

                    Vec3d point = new Vec3d(points3.get(i).x, firstPoint.y, points3.get(i).z);
                    point.sub(centerPoint);

                    double dot = point.dot(sphereOrigin);
                    double det = ((point.x * sphereOrigin.z) - (point.z * sphereOrigin.x));
                    double angle = Math.toDegrees(Math.atan2(det, dot));

                    if (angle < 0)
                        angle += 360;

                    double time = angle / 360;
                    if (!clockwise)
                        time = 1 - time;
                    for (int j = 0; j < times.size(); j++) {
                        if (times.get(j) > time) {
                            times.add(j, time);
                            vecs.add(j, new Vec1d(points3.get(i).y));
                            newPointsSorted.add(j, points3.get(i));
                            break;
                        }
                    }
                    newPointsSorted.add(points3.get(i));
                    times.add(time);
                    vecs.add(new Vec1d(points3.get(i).y));
                }

                if (scene.loop == 0)
                    newPointsSorted.add(newPointsSorted.get(0).copy());

                times.add(1D);
                vecs.add(new Vec1d(firstPoint.y));

                return (Interpolation<T>) new CircularInterpolation(clockwise, (List<Vec3d>) points, scene.lookTarget, sphereOrigin, radius, new HermiteInterpolation<>(ArrayUtils
                        .toPrimitive(times.toArray(new Double[0])), vecs.toArray(new Vec1d[0])));
            }
            return null;
        });
    }

    @Override
    public <T extends VecNd> Interpolation<T> create(double[] timed, CamScene scene, T before, List<T> points, T after, CamAttribute<T> attribute) {
        return EnvExecutor.safeCallWhenOn(EnvType.CLIENT, () -> () -> {
            if (attribute == CamAttribute.POSITION && scene.lookTarget != null) {
                Interpolation<T> result = createClient(timed, scene, before, points, after, attribute);
                if (result != null)
                    return result;
            }
            return new HermiteInterpolation<T>(points);
        });
    }
}
