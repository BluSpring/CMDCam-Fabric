package com.creativemd.cmdcam.client.interpolation;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import com.creativemd.cmdcam.client.PathParseException;
import com.creativemd.cmdcam.common.utils.CamPoint;
import com.creativemd.cmdcam.common.utils.CamTarget;
import com.creativemd.creativecore.common.utils.math.interpolation.HermiteInterpolation;
import com.creativemd.creativecore.common.utils.math.vec.Vec1;
import com.creativemd.creativecore.common.utils.math.vec.Vec3;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;

public class CircularMovement extends HermiteMovement {
	
	private static Minecraft mc = Minecraft.getMinecraft();
	
	public Vector3d sphereOrigin;
	public double radius;
	public CamTarget target;
	public HermiteInterpolation<Vec1> yAxis;
	
	@Override
	public void initMovement(List<CamPoint> points, int loops, CamTarget target) throws PathParseException {
		if (target == null)
			throw new PathParseException("No target found");
		
		Vec3d center = target.getTargetVec(mc.world, mc.getRenderPartialTicks());
		if (center != null) {
			points.add(points.get(0));
			
			this.target = target;
			Vector3d firstPoint = new Vector3d(points.get(0).x, points.get(0).y, points.get(0).z);
			Vector3d centerPoint = new Vector3d(center.x, center.y, center.z);
			this.sphereOrigin = new Vector3d(firstPoint);
			sphereOrigin.sub(centerPoint);
			
			this.radius = sphereOrigin.length();
			
			ArrayList<Vec1> vecs = new ArrayList<>();
			ArrayList<Double> times = new ArrayList<>();
			
			times.add(0D);
			vecs.add(new Vec1(firstPoint.y));
			
			ArrayList<CamPoint> newPointsSorted = new ArrayList<>();
			newPointsSorted.add(points.get(0));
			
			for (int i = 1; i < points.size() - 1; i++) {
				
				Vector3d point = new Vector3d(points.get(i).x, firstPoint.y, points.get(i).z);
				point.sub(centerPoint);
				
				double dot = point.dot(sphereOrigin);
				double det = ((point.x * sphereOrigin.z) - (point.z * sphereOrigin.x));
				double angle = Math.toDegrees(Math.atan2(det, dot));
				
				if (angle < 0)
					angle += 360;
				
				double time = angle / 360;
				for (int j = 0; j < times.size(); j++) {
					if (times.get(j) > time) {
						times.add(j, time);
						vecs.add(j, new Vec1(points.get(i).y));
						newPointsSorted.add(j, points.get(i));
						break;
					}
				}
				newPointsSorted.add(points.get(i));
				times.add(time);
				vecs.add(new Vec1(points.get(i).y));
			}
			
			if (loops == 0)
				newPointsSorted.add(newPointsSorted.get(0).copy());
			
			times.add(1D);
			vecs.add(new Vec1(firstPoint.y));
			
			this.yAxis = new HermiteInterpolation<>(times.toArray(new Double[0]), vecs.toArray(new Vec1[0]));
			
			super.initMovement(times.toArray(new Double[0]), newPointsSorted, loops, target);
		} else
			throw new PathParseException("Invalid target");
	}
	
	@Override
	public CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress, boolean isFirstLoop, boolean isLastLoop) {
		CamPoint newCamPoint = super.getPointInBetween(point1, point2, percent, wholeProgress, isFirstLoop, isLastLoop);
		
		double angle = wholeProgress * 360;
		
		Vec3d center = target.getTargetVec(mc.world, mc.getRenderPartialTicks());
		if (center != null) {
			Vector3d centerPoint = new Vector3d(center.x, center.y, center.z);
			
			Vector3d newPoint = new Vector3d(sphereOrigin);
			newPoint.y = 0;
			Matrix3d matrix = new Matrix3d();
			matrix.rotY(Math.toRadians(angle));
			matrix.transform(newPoint);
			
			newPoint.y = yAxis.valueAt(wholeProgress).x - center.y;
			newPoint.normalize();
			newPoint.scale(radius);
			
			newPoint.add(centerPoint);
			newCamPoint.x = newPoint.x;
			newCamPoint.y = newPoint.y;
			newCamPoint.z = newPoint.z;
		}
		
		return newCamPoint;
	}
	
	@Override
	public Vec3 getColor() {
		return new Vec3(1, 1, 0);
	}
	
}
