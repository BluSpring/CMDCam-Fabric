package team.creative.cmdcam.client;

import java.util.List;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public interface EntitySelectorClient {
    
    public Entity findSingleEntityClient(FabricClientCommandSource source) throws CommandSyntaxException;
    
    public List<? extends Entity> findEntitiesClient(FabricClientCommandSource source) throws CommandSyntaxException;
    
    public Player findSinglePlayerClient(FabricClientCommandSource source) throws CommandSyntaxException;
    
    public List<Player> findPlayersClient(FabricClientCommandSource source) throws CommandSyntaxException;
    
}
