package team.creative.cmdcam.server;

import net.minecraft.server.level.ServerPlayer;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.packet.ConnectPacket;

public class CamEventHandler {

    public void onPlayerConnect(ServerPlayer player) {
        CMDCam.NETWORK.sendToClient(new ConnectPacket(), player);
    }
    
}
