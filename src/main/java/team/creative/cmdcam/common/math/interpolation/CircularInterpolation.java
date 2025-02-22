package team.creative.cmdcam.common.math.interpolation;

import net.minecraft.client.Minecraft;
import team.creative.cmdcam.common.target.CamTarget;
import team.creative.creativecore.common.util.math.interpolation.HermiteInterpolation;
import team.creative.creativecore.common.util.math.matrix.Matrix3;
import team.creative.creativecore.common.util.math.vec.Vec1d;
import team.creative.creativecore.common.util.math.vec.Vec3d;

import java.util.List;

public class CircularInterpolation extends HermiteInterpolation<Vec3d> {

    public Vec3d sphereOrigin;
    public double radius;
    public CamTarget target;
    public HermiteInterpolation<Vec1d> yAxis;
    public final boolean clockwise;

    public CircularInterpolation(boolean clockwise, List<Vec3d> points, CamTarget target, Vec3d sphereOrigin, double radius, HermiteInterpolation<Vec1d> yAxis) {
        super(points);
        this.clockwise = clockwise;
        this.target = target;
        this.sphereOrigin = sphereOrigin;
        this.radius = radius;
        this.yAxis = yAxis;
    }

    @Override
    public Vec3d valueAt(double t) {
        Minecraft mc = Minecraft.getInstance();
        Vec3d center = target.position(mc.level, mc.getDeltaFrameTime());
        if (center != null) {
            Vec3d centerPoint = new Vec3d(center.x, center.y, center.z);

            if (!clockwise)
                t = 1 - t;
            double angle = t * 360;

            Vec3d newPoint = new Vec3d(sphereOrigin);
            newPoint.y = 0;
            Matrix3 matrix = new Matrix3();
            matrix.rotY(Math.toRadians(angle));
            matrix.transform(newPoint);

            newPoint.y = yAxis.valueAt(t).x - center.y;
            newPoint.normalize();
            newPoint.scale(radius);

            newPoint.add(centerPoint);
            return newPoint;
        }
        return super.valueAt(t);
    }

}
