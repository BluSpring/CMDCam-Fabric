package team.creative.cmdcam;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.fabricators_of_create.porting_lib.features.LevelExtensions;
import io.github.fabricators_of_create.porting_lib.features.entity.MultiPartEntity;
import io.github.fabricators_of_create.porting_lib.features.entity.PartEntity;
import io.github.fabricators_of_create.porting_lib.features.registry.LazyRegistrar;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.creative.cmdcam.common.command.argument.CamModeArgument;
import team.creative.cmdcam.common.command.argument.CamPitchModeArgument;
import team.creative.cmdcam.common.command.argument.DurationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument.AllInterpolationArgument;
import team.creative.cmdcam.common.command.builder.SceneCommandBuilder;
import team.creative.cmdcam.common.command.builder.SceneStartCommandBuilder;
import team.creative.cmdcam.common.packet.*;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.fabric.ArgumentTypeInfosHelper;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.cmdcam.server.CamEventHandler;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.network.CreativeNetwork;
import team.creative.creativecore.common.network.CreativePacket;

import java.util.Collection;

@Mod(value = CMDCam.MODID)
public class CMDCam implements ModInitializer {
    
    public static final String MODID = "cmdcam";
    
    private static final Logger LOGGER = LogManager.getLogger(CMDCam.MODID);
    public static final CreativeNetwork NETWORK = new CreativeNetwork(1, LOGGER, new ResourceLocation(CMDCam.MODID, "main"));
    public static final CMDCamConfig CONFIG = new CMDCamConfig();
    public static final LazyRegistrar<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = LazyRegistrar.create(Registries.COMMAND_ARGUMENT_TYPE, MODID);

    private void initPartEntity(){
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof MultiPartEntity partEntity && partEntity.isMultipartEntity()) {
                PartEntity<?>[] parts = partEntity.getParts();
                if (parts != null) {
                    for (PartEntity<?> part : parts) {
                        ((LevelExtensions)(world)).getPartEntityMap().put(part.getId(), part);
                    }
                }
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof MultiPartEntity partEntity && partEntity.isMultipartEntity()) {
                PartEntity<?>[] parts = partEntity.getParts();
                if (parts != null) {
                    for (PartEntity<?> part : parts) {
                        ((LevelExtensions)world).getPartEntityMap().remove(part.getId());
                    }
                }
            }
        });
    }

    public void onInitialize() {
        this.initPartEntity();
        this.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            commands(dispatcher);
        });

        COMMAND_ARGUMENT_TYPES.register("duration", () -> ArgumentTypeInfosHelper.registerByClass(DurationArgument.class, SingletonArgumentInfo.<DurationArgument>contextFree(
            () -> DurationArgument.duration())));
        COMMAND_ARGUMENT_TYPES.register("cam_mode", () -> ArgumentTypeInfosHelper.registerByClass(CamModeArgument.class, SingletonArgumentInfo.<CamModeArgument>contextFree(
            () -> CamModeArgument.mode())));
        COMMAND_ARGUMENT_TYPES.register("interpolation", () -> ArgumentTypeInfosHelper.registerByClass(InterpolationArgument.class, SingletonArgumentInfo
                .<InterpolationArgument>contextFree(() -> InterpolationArgument.interpolation())));
        COMMAND_ARGUMENT_TYPES.register("all_interpolation", () -> ArgumentTypeInfosHelper.registerByClass(AllInterpolationArgument.class, SingletonArgumentInfo
                .<AllInterpolationArgument>contextFree(() -> InterpolationArgument.interpolationAll())));
        COMMAND_ARGUMENT_TYPES.register("pitch_mode", () -> ArgumentTypeInfosHelper.registerByClass(CamPitchModeArgument.class, SingletonArgumentInfo.<CamPitchModeArgument>contextFree(
            () -> CamPitchModeArgument.pitchMode())));
        COMMAND_ARGUMENT_TYPES.register();
    }
    
    private void init() {
        NETWORK.registerType(ConnectPacket.class, ConnectPacket::new);
        NETWORK.registerType(GetPathPacket.class, GetPathPacket::new);
        NETWORK.registerType(SetPathPacket.class, SetPathPacket::new);
        NETWORK.registerType(StartPathPacket.class, StartPathPacket::new);
        NETWORK.registerType(StopPathPacket.class, StopPathPacket::new);
        NETWORK.registerType(TeleportPathPacket.class, TeleportPathPacket::new);
        NETWORK.registerType(PausePathPacket.class, PausePathPacket::new);
        NETWORK.registerType(ResumePathPacket.class, ResumePathPacket::new);
        
        new CamEventHandler();
        
        CreativeConfigRegistry.ROOT.registerValue(MODID, CONFIG);
    }
    
    private void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> camServer = Commands.literal("cam-server");
        
        SceneStartCommandBuilder.start(camServer, CMDCamServer.PROCESSOR);
        
        LiteralArgumentBuilder<CommandSourceStack> get = Commands.literal("get");
        SceneCommandBuilder.scene(get, CMDCamServer.PROCESSOR);
        camServer.then(get);
        
        dispatcher.register(camServer.then(Commands.literal("stop").then(Commands.argument("players", EntityArgument.players()).executes(x -> {
            CreativePacket packet = new StopPathPacket();
            for (ServerPlayer player : EntityArgument.getPlayers(x, "players"))
                CMDCam.NETWORK.sendToClient(packet, player);
            return 0;
        }))).then(Commands.literal("pause").then(Commands.argument("players", EntityArgument.players()).executes(x -> {
            CreativePacket packet = new PausePathPacket();
            for (ServerPlayer player : EntityArgument.getPlayers(x, "players"))
                CMDCam.NETWORK.sendToClient(packet, player);
            return 0;
        }))).then(Commands.literal("resume").then(Commands.argument("players", EntityArgument.players()).executes(x -> {
            CreativePacket packet = new ResumePathPacket();
            for (ServerPlayer player : EntityArgument.getPlayers(x, "players"))
                CMDCam.NETWORK.sendToClient(packet, player);
            return 0;
        }))).then(Commands.literal("list").executes((x) -> {
            Collection<String> names = CMDCamServer.getSavedPaths(x.getSource().getLevel());
            x.getSource().sendSystemMessage(Component.translatable("scenes.list", names.size(), String.join(", ", names)));
            return 0;
        })).then(Commands.literal("clear").executes((x) -> {
            CMDCamServer.clearPaths(x.getSource().getLevel());
            x.getSource().sendSuccess(() -> Component.translatable("scenes.clear"), true);
            return 0;
        })).then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.string()).executes((x) -> {
            String name = StringArgumentType.getString(x, "name");
            if (CMDCamServer.removePath(x.getSource().getLevel(), name))
                x.getSource().sendSuccess(() -> Component.translatable("scene.remove", name), true);
            else
                x.getSource().sendFailure(Component.translatable("scene.remove_fail", name));
            return 0;
        }))).then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.string()).executes((x) -> {
            String name = StringArgumentType.getString(x, "name");
            if (CMDCamServer.get(x.getSource().getLevel(), name) != null)
                x.getSource().sendSuccess(() -> Component.translatable("scene.exists", name), true);
            else {
                CMDCamServer.set(x.getSource().getLevel(), name, CamScene.createDefault());
                x.getSource().sendSuccess(() -> Component.translatable("scene.create", name), true);
            }
            return 0;
        }))));
    }
}
