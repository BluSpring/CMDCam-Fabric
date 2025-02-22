package team.creative.cmdcam.common.scene.mode;

import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.run.CamRun;
import team.creative.cmdcam.common.target.CamTarget.SelfTarget;
import team.creative.cmdcam.common.utils.EnvExecutor;
import team.creative.creativecore.common.util.math.vec.Vec3d;

public class DefaultMode extends CamMode {
    
    public DefaultMode(CamScene scene) {
        super(scene);
        if (scene.lookTarget instanceof SelfTarget)
            scene.lookTarget = null;
        if (scene.posTarget instanceof SelfTarget)
            scene.posTarget = null;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void process(CamPoint point) {
        super.process(point);
        EnvExecutor.safeRunWhenOn(EnvType.CLIENT, () -> () -> {
            Minecraft.getInstance().mouseHandler.grabMouse();
        });
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void finished(CamRun run) {
        super.finished(run);
        EnvExecutor.safeRunWhenOn(EnvType.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if (!mc.player.isCreative())
                mc.player.getAbilities().flying = false;
        });
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public Entity getCamera() {
        return EnvExecutor.safeCallWhenOn(EnvType.CLIENT, () -> () -> Minecraft.getInstance().player);
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void correctTargetPosition(Vec3d vec) {
        EnvExecutor.safeRunWhenOn(EnvType.CLIENT, () -> () -> {
            vec.y -= Minecraft.getInstance().player.getEyeHeight();
        });
    }
    
    @Override
    public boolean outside() {
        return false;
    }
    
}
