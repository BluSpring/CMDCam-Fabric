package team.creative.cmdcam.common.command.builder.client;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.command.CamCommandProcessor;
import team.creative.cmdcam.common.target.CamTarget;
import team.creative.cmdcam.fabric.PosArgHelper;

import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class TargetArgumentBuilder extends ArgumentBuilder<FabricClientCommandSource, TargetArgumentBuilder> {
    
    private final String literal;
    private final boolean look;
    private final CamCommandProcessor processor;
    
    public TargetArgumentBuilder(final String literal, boolean look, CamCommandProcessor processor) {
        this.literal = literal;
        this.look = look;
        this.processor = processor;
    }
    
    public String getLiteral() {
        return literal;
    }
    
    @Override
    protected TargetArgumentBuilder getThis() {
        return this;
    }
    
    private String translatePrefix() {
        if (look)
            return "scene.look.target.";
        return "scene.follow.target.";
    }
    
    @Override
    public CommandNode<FabricClientCommandSource> build() {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommands.literal(literal).executes(x -> {
            var target = look ? processor.getScene(x).lookTarget : processor.getScene(x).posTarget;
            x.getSource().sendFeedback(Component.translatable(look ? "scene.output.look" : "scene.output.follow", target == null ? "none" : target.print(x.getSource()
                    .getLevel())));
            return 0;
        }).then(ClientCommands.literal("none").executes(x -> {
            try {
                processor.setTarget(x, null, look);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable(translatePrefix() + "remove"));
            return 0;
        })).then(ClientCommands.literal("self").executes(x -> {
            try {
                processor.setTarget(x, new CamTarget.SelfTarget(), look);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable(translatePrefix() + "self"));
            return 0;
        })).then(ClientCommands.literal("player").then(ClientCommands.argument("player", EntityArgument.player()).executes(x -> {
            Player player = processor.getPlayer(x, "player");
            try {
                processor.setTarget(x, new CamTarget.PlayerTarget(player), look);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable(translatePrefix() + "player", player.getScoreboardName()));
            return 0;
        }))).then(ClientCommands.literal("entity").then(ClientCommands.argument("entity", EntityArgument.entity()).executes(x -> {
            Entity entity = processor.getEntity(x, "entity");
            try {
                processor.setTarget(x, new CamTarget.EntityTarget(entity), look);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable(translatePrefix() + "entity", entity.getStringUUID()));
            return 0;
        }))).then(ClientCommands.literal("pos").then(ClientCommands.argument("pos", BlockPosArgument.blockPos()).executes(x -> {
            BlockPos pos = PosArgHelper.getLoadedBlockPos(x, "pos");
            try {
                processor.setTarget(x, new CamTarget.BlockTarget(pos), look);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable(translatePrefix() + "pos", pos.toShortString()));
            return 0;
        })));
        
        if (processor.canSelectTarget())
            builder.then(ClientCommands.literal("select").executes(x -> {
                try {
                    processor.selectTarget(x, look);
                } catch (SceneException e) {
                    x.getSource().sendError(Component.translatable(e.getMessage()));
                }
                x.getSource().sendFeedback(Component.translatable(translatePrefix() + "select"));
                return 0;
            }));
        
        return builder.build();
    }
    
}
