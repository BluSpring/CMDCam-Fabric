package team.creative.cmdcam.common.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.target.CamTarget;
import team.creative.creativecore.common.util.math.vec.Vec3d;

import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface CamCommandProcessor<T extends SharedSuggestionProvider> {
    
    public CamScene getScene(CommandContext<T> context);
    
    public boolean canSelectTarget();
    
    public void selectTarget(CommandContext<T> context, boolean look) throws SceneException;
    
    public default void setTarget(CommandContext<T> context, CamTarget target, boolean look) throws SceneException {
        if (look)
            getScene(context).lookTarget = target;
        else {
            checkFollowTarget(context, target != null);
            getScene(context).posTarget = target;
        }
    }
    
    public default void checkFollowTarget(CommandContext<T> context, boolean shouldFollow) throws SceneException {
        CamScene scene = getScene(context);
        if (scene.points.isEmpty())
            return;
        if (shouldFollow && scene.posTarget == null)
            throw new SceneException("scene.follow.absolute_fail");
        if (!shouldFollow && scene.posTarget != null)
            throw new SceneException("scene.follow.relative_fail");
    }
    
    public boolean canCreatePoint(CommandContext<T> context);
    
    public CamPoint createPoint(CommandContext<T> context);
    
    public default void makeRelative(CamScene scene, Level level, CamPoint point) throws SceneException {
        if (scene.posTarget != null) {
            Vec3d vec = scene.posTarget.position(scene.run);
            if (vec == null)
                throw new SceneException("scene.follow.not_found");
            point.sub(vec);
        }
    }
    
    public boolean requiresSceneName();
    
    public boolean requiresPlayer();
    
    public void start(CommandContext<T> context) throws SceneException;
    
    public void teleport(CommandContext<T> context, int index);
    
    public void markDirty(CommandContext<T> context);
    
    public Player getPlayer(CommandContext<T> context, String name) throws CommandSyntaxException;
    
    public Entity getEntity(CommandContext<T> context, String name) throws CommandSyntaxException;
    
}
