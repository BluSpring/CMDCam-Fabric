package team.creative.cmdcam.common.command.builder.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import team.creative.cmdcam.common.command.CamCommandProcessor;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;

import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class FollowArgumentBuilder extends ArgumentBuilder<FabricClientCommandSource, FollowArgumentBuilder> {
    
    private final CamAttribute attribute;
    private final CamCommandProcessor processor;
    
    public FollowArgumentBuilder(CamAttribute attribute, CamCommandProcessor processor) {
        this.attribute = attribute;
        this.processor = processor;
    }
    
    @Override
    protected FollowArgumentBuilder getThis() {
        return this;
    }
    
    @Override
    public CommandNode<FabricClientCommandSource> build() {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommands.literal(attribute.name())
                .then(ClientCommands.literal("step").then(ClientCommands.argument("div", DoubleArgumentType.doubleArg(1)).executes(x -> {
                    double div = DoubleArgumentType.getDouble(x, "div");
                    processor.getScene(x).getConfig(attribute).div = div;
                    processor.markDirty(x);
                    x.getSource().sendFeedback(Component.translatable("scene.follow.div", attribute.name(), div));
                    return 0;
                })));
        
        return builder.build();
    }
    
}
