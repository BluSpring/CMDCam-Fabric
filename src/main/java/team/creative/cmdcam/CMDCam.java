package team.creative.cmdcam;

import java.util.Collection;
import java.util.function.Supplier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.creative.cmdcam.common.command.argument.CamModeArgument;
import team.creative.cmdcam.common.command.argument.CamPitchModeArgument;
import team.creative.cmdcam.common.command.argument.DurationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument.AllInterpolationArgument;
import team.creative.cmdcam.common.command.builder.SceneCommandBuilder;
import team.creative.cmdcam.common.command.builder.SceneStartCommandBuilder;
import team.creative.cmdcam.common.packet.ConnectPacket;
import team.creative.cmdcam.common.packet.GetPathPacket;
import team.creative.cmdcam.common.packet.PausePathPacket;
import team.creative.cmdcam.common.packet.ResumePathPacket;
import team.creative.cmdcam.common.packet.SetPathPacket;
import team.creative.cmdcam.common.packet.StartPathPacket;
import team.creative.cmdcam.common.packet.StopPathPacket;
import team.creative.cmdcam.common.packet.TeleportPathPacket;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.cmdcam.server.CamEventHandler;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.network.CreativeNetwork;
import team.creative.creativecore.common.network.CreativePacket;
import team.creative.creativecore.mixin.ArgumentTypeInfosAccessor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public class CMDCam implements ModInitializer {

    public static final String MODID = "cmdcam";

    private static final Logger LOGGER = LogManager.getLogger(CMDCam.MODID);
    public static final CreativeNetwork NETWORK = new CreativeNetwork(1, LOGGER, Identifier.tryBuild(CMDCam.MODID, "main"));
    public static final CMDCamConfig CONFIG = new CMDCamConfig();
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, MODID);

    public record DeferredRegister<T>(
        String modId,
        Registry<T> registry
    ) {

        public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String modId) {
            var registry = (Registry<T>) BuiltInRegistries.REGISTRY.getValueOrThrow((ResourceKey) registryKey);
            return new DeferredRegister<>(modId, registry);
        }

        public <U extends T> void register(String name, Supplier<U> builder) {
            Registry.register(this.registry(), Identifier.fromNamespaceAndPath(this.modId(), name), builder.get());
        }

        public void register() {}
    }

    @Override
    public void onInitialize() {
        this.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> commands(dispatcher));

        COMMAND_ARGUMENT_TYPES.register("duration", () -> registerByClass(DurationArgument.class, SingletonArgumentInfo.<DurationArgument>contextFree(
            () -> DurationArgument.duration())));
        COMMAND_ARGUMENT_TYPES.register("cam_mode", () -> registerByClass(CamModeArgument.class, SingletonArgumentInfo.<CamModeArgument>contextFree(
            () -> CamModeArgument.mode())));
        COMMAND_ARGUMENT_TYPES.register("interpolation", () -> registerByClass(InterpolationArgument.class, SingletonArgumentInfo
            .<InterpolationArgument>contextFree(() -> InterpolationArgument.interpolation())));
        COMMAND_ARGUMENT_TYPES.register("all_interpolation", () -> registerByClass(AllInterpolationArgument.class, SingletonArgumentInfo
            .<AllInterpolationArgument>contextFree(() -> InterpolationArgument.interpolationAll())));
        COMMAND_ARGUMENT_TYPES.register("pitch_mode", () -> registerByClass(CamPitchModeArgument.class, SingletonArgumentInfo.<CamPitchModeArgument>contextFree(
            () -> CamPitchModeArgument.pitchMode())));
        COMMAND_ARGUMENT_TYPES.register();
    }

    public static synchronized <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> I registerByClass(Class<A> infoClass, I argumentTypeInfo) {
        ArgumentTypeInfosAccessor.getByClass().put(infoClass, argumentTypeInfo);
        return argumentTypeInfo;
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

        var handler = new CamEventHandler();
        ServerPlayerEvents.JOIN.register(handler::onPlayerConnect);

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
