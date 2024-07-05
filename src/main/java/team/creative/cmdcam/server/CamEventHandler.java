package team.creative.cmdcam.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.packet.ConnectPacket;

public class CamEventHandler {
    public CamEventHandler() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            onPlayerConnect(handler.getPlayer());
        });
    }

    public void onPlayerConnect(ServerPlayer player) {
        CMDCam.NETWORK.sendToClient(new ConnectPacket(), player);
    }
    
}
