package team.creative.cmdcam.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.fabricators_of_create.porting_lib.common.util.MinecraftClientUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.client.command.builder.ClientPointArgumentBuilder;
import team.creative.cmdcam.client.command.builder.ClientSceneCommandBuilder;
import team.creative.cmdcam.client.command.builder.ClientSceneStartCommandBuilder;
import team.creative.cmdcam.client.mixin.MinecraftAccessor;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.packet.GetPathPacket;
import team.creative.cmdcam.common.packet.SetPathPacket;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.creativecore.client.CreativeCoreClient;

import java.util.HashMap;
import java.util.List;

public class CMDCamClient implements ClientModInitializer {
    
    public final static Minecraft mc = Minecraft.getInstance();
    public static final ClientCamCommandProcessorClient PROCESSOR = new ClientCamCommandProcessorClient();
    public static final HashMap<String, CamScene> SCENES = new HashMap<>();
    
    private static final CamScene scene = CamScene.createDefault();
    private static CamScene playing;
    private static boolean serverAvailable = false;
    private static boolean hideGuiCache;
    private static boolean hasTargetMarker;
    private static CamPoint targetMarker;
    
    public static void resetServerAvailability() {
        serverAvailable = false;
    }
    
    public static void setServerAvailability() {
        serverAvailable = true;
    }
    
    public void onInitializeClient() {
        new CamEventHandlerClient();
        CreativeCoreClient.registerClientConfig(CMDCam.MODID);
        //ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
            //() -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (a, b) -> true));

        load();
    }
    
    public static void load() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            CMDCamClient.commands(dispatcher);
        });
        KeyHandler.registerKeys();
    }
    
    public static void commands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> cam = ClientCommandManager.literal("cam");
        
        ClientSceneStartCommandBuilder.start(cam, PROCESSOR);

        ClientSceneCommandBuilder.scene(cam, PROCESSOR);
        
        dispatcher.register(cam.then(ClientCommandManager.literal("stop").executes(x -> {
            CMDCamClient.stop();
            return 0;
        })).then(ClientCommandManager.literal("pause").executes(x -> {
            CMDCamClient.pause();
            return 0;
        })).then(ClientCommandManager.literal("resume").executes(x -> {
            CMDCamClient.resume();
            return 0;
        })).then(ClientCommandManager.literal("show").then(ClientCommandManager.argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
            String interpolation = StringArgumentType.getString(x, "interpolation");
            if (!interpolation.equalsIgnoreCase("all")) {
                CamInterpolation.REGISTRY.get(interpolation).isRenderingEnabled = true;
                x.getSource().sendFeedback(Component.translatable("scene.interpolation.show", interpolation));
            } else {
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    movement.isRenderingEnabled = true;
                x.getSource().sendFeedback(Component.translatable("scene.interpolation.show_all"));
            }
            return 0;
        }))).then(ClientCommandManager.literal("hide").then(ClientCommandManager.argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
            String interpolation = StringArgumentType.getString(x, "interpolation");
            if (!interpolation.equalsIgnoreCase("all")) {
                CamInterpolation.REGISTRY.get(interpolation).isRenderingEnabled = false;
                x.getSource().sendFeedback(Component.translatable("scene.interpolation.hide", interpolation));
            } else {
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    movement.isRenderingEnabled = false;
                x.getSource().sendFeedback(Component.translatable("scene.interpolation.hide_all"));
            }
            return 0;
        }))).then(ClientCommandManager.literal("list").executes((x) -> {
            if (CMDCamClient.serverAvailable) {
                x.getSource().sendError(Component.translatable("scenes.list_fail"));
                return 0;
            }
            x.getSource().sendFeedback(Component.translatable("scenes.list", SCENES.size(), String.join(", ", SCENES.keySet())));
            return 0;
        })).then(ClientCommandManager.literal("load").then(ClientCommandManager.argument("path", StringArgumentType.string()).executes((x) -> {
            String pathArg = StringArgumentType.getString(x, "path");
            if (CMDCamClient.serverAvailable)
                CMDCam.NETWORK.sendToServer(new GetPathPacket(pathArg));
            else {
                CamScene scene = CMDCamClient.SCENES.get(pathArg);
                if (scene != null) {
                    set(scene);
                    x.getSource().sendFeedback(Component.translatable("scenes.load", pathArg));
                } else
                    x.getSource().sendError(Component.translatable("scenes.load_fail", pathArg));
            }
            return 0;
        }))).then(ClientCommandManager.literal("save").then(ClientCommandManager.argument("path", StringArgumentType.string()).executes((x) -> {
            String pathArg = StringArgumentType.getString(x, "path");
            try {
                CamScene scene = CMDCamClient.createScene();
                
                if (CMDCamClient.serverAvailable)
                    CMDCam.NETWORK.sendToServer(new SetPathPacket(pathArg, scene));
                else {
                    CMDCamClient.SCENES.put(pathArg, scene);
                    x.getSource().sendFeedback(Component.translatable("scenes.save", pathArg));
                }
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            return 0;
        }))).then(new ClientPointArgumentBuilder("follow_center", (x, y) -> targetMarker = y, PROCESSOR).executes(x -> {
            targetMarker = CamPoint.createLocal();
            return 0;
        })));
        
    }
    
    public static CamScene getScene() {
        if (isPlaying())
            return playing;
        return scene;
    }
    
    public static CamScene getConfigScene() {
        return scene;
    }
    
    public static boolean isPlaying() {
        return playing != null;
    }
    
    public static List<CamPoint> getPoints() {
        return scene.points;
    }
    
    public static void set(CamScene scene) {
        CMDCamClient.scene.set(scene);
        checkTargetMarker();
    }
    
    public static void checkTargetMarker() {
        hasTargetMarker = scene.posTarget != null;
        if (hasTargetMarker && targetMarker == null)
            targetMarker = CamPoint.createLocal();
    }
    
    public static void start(CamScene scene) {
        if (scene.points.isEmpty())
            return;
        if (scene.points.size() == 1)
            scene.points.add(scene.points.get(0));
        playing = scene;
        playing.play();
    }
    
    public static void pause() {
        if (playing != null)
            playing.pause();
        mc.options.hideGui = hideGuiCache;
    }
    
    public static void resume() {
        if (playing != null)
            playing.resume();
    }
    
    public static void stop() {
        if (playing == null)
            return;
        if (playing.serverSynced())
            return;
        playing.finish(mc.level);
        playing = null;
        mc.options.hideGui = hideGuiCache;
    }
    
    public static void stopServer() {
        if (playing == null)
            return;
        playing.finish(mc.level);
        playing = null;
        mc.options.hideGui = hideGuiCache;
    }
    
    public static void noTickPath(Level level, float renderTickTime) {
        hideGuiCache = mc.options.hideGui;
    }
    
    public static void gameTickPath(Level level) {
        playing.gameTick(level);
    }
    
    public static void renderTickPath(Level level, float renderTickTime) {
        playing.renderTick(level, renderTickTime);
        if (!playing.playing()) {
            mc.options.hideGui = hideGuiCache;
            playing = null;
        }
    }
    
    public static void resetTargetMarker() {
        targetMarker = null;
    }
    
    public static boolean hasTargetMarker() {
        return hasTargetMarker && targetMarker != null && scene.posTarget != null;
    }
    
    public static CamPoint getTargetMarker() {
        return targetMarker;
    }
    
    public static CamScene createScene() throws SceneException {
        if (scene.points.size() < 1)
            throw new SceneException("scene.create_fail");
        
        CamScene newScene = scene.copy();
        if (newScene.points.size() == 1)
            newScene.points.add(newScene.points.get(0));
        return newScene;
    }
    
    public static void teleportTo(CamPoint point) {
        Minecraft mc = Minecraft.getInstance();
        mc.player.getAbilities().flying = true;

        var partialTick = mc.isPaused() ? MinecraftClientUtil.getRenderPartialTicksPaused(mc) : ((MinecraftAccessor) mc).getTimer().partialTick;
        CamEventHandlerClient.roll((float) point.roll);
        CamEventHandlerClient.fov(point.zoom - CamEventHandlerClient.fovExactVanilla(partialTick));
        mc.player.absMoveTo(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        mc.player.absMoveTo(point.x, point.y - mc.player.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
    }
    
}
