package team.creative.cmdcam.server;

import java.util.ArrayList;
import java.util.Collection;

import team.creative.cmdcam.common.scene.CamScene;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedDataType;

public class CMDCamServer {
    
    public static final CamCommandProcessorServer PROCESSOR = new CamCommandProcessorServer();
    
    private static final SavedDataType<CamSaveData> FACTORY = new SavedDataType<>(CamSaveData.DATA_NAME, CamSaveData::new, CamSaveData.CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    
    public static CamScene get(Level level, String name) {
        CamSaveData data = ((ServerLevel) level).getDataStorage().get(FACTORY);
        if (data != null)
            return data.get(name);
        return null;
    }
    
    public static void set(Level level, String name, CamScene scene) {
        CamSaveData data = ((ServerLevel) level).getDataStorage().get(FACTORY);
        if (data == null) {
            data = new CamSaveData();
            ((ServerLevel) level).getDataStorage().set(FACTORY, data);
        }
        data.set(name, scene);
    }
    
    public static void markDirty(Level level) {
        ((ServerLevel) level).getDataStorage().get(FACTORY).setDirty();
    }
    
    public static boolean removePath(Level level, String name) {
        CamSaveData data = ((ServerLevel) level).getDataStorage().get(FACTORY);
        if (data != null)
            return data.remove(name);
        return false;
    }
    
    public static Collection<String> getSavedPaths(Level level) {
        CamSaveData data = ((ServerLevel) level).getDataStorage().get(FACTORY);
        if (data != null)
            return data.names();
        return new ArrayList<>();
    }
    
    public static void clearPaths(Level level) {
        CamSaveData data = ((ServerLevel) level).getDataStorage().get(FACTORY);
        if (data != null)
            data.clear();
    }
    
}
