package team.creative.cmdcam.common.scene.mode;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.run.CamRun;
import team.creative.cmdcam.common.utils.EnvExecutor;
import team.creative.creativecore.common.util.mc.TickUtils;

public class ClientOutsideMode extends OutsideMode {

    @Environment(EnvType.CLIENT)
    public Entity camPlayer;

    public ClientOutsideMode(CamScene scene) {
        super(scene);
    }
    
    @Override
    @Environment(EnvType.CLIENT)
    public void started(CamRun run) {
        EnvExecutor.safeRunWhenOn(EnvType.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            Vec3 vec = mc.player.getEyePosition(TickUtils.getFrameTime(mc.level));
            this.camPlayer = new ItemEntity(mc.level, vec.x, vec.y, vec.z, ItemStack.EMPTY);
            this.camPlayer.setOldPosAndRot();
        });
    }
    
    @Override
    @Environment(EnvType.CLIENT)
    public void finished(CamRun run) {
        super.finished(run);
        EnvExecutor.safeRunWhenOn(EnvType.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.cameraEntity = mc.player;
        });
    }
    
    @Override
    @Environment(EnvType.CLIENT)
    public Entity getCamera() {
        return EnvExecutor.safeCallWhenOn(EnvType.CLIENT, () -> () -> camPlayer);
    }
    
    @Override
    @Environment(EnvType.CLIENT)
    public void process(CamPoint point) {
        super.process(point);
        EnvExecutor.safeRunWhenOn(EnvType.CLIENT, () -> () -> {
            Minecraft.getInstance().cameraEntity = camPlayer;
        });
    }
    
    @Override
    public boolean outside() {
        return true;
    }
    
}
