package team.creative.cmdcam.server;

import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.packet.ConnectPacket;

import net.minecraft.server.level.ServerPlayer;

public class CamEventHandler {

    public void onPlayerConnect(ServerPlayer player) {
        CMDCam.NETWORK.sendToClient(new ConnectPacket(), player);
    }

}
