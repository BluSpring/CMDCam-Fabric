package team.creative.cmdcam.client.mixin;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import team.creative.cmdcam.client.EntitySelectorClient;

import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin implements EntitySelectorClient {

    @Shadow
    @Final
    private int maxResults;
    @Shadow
    @Final
    private boolean includesEntities;
    @Shadow
    @Final
    private boolean worldLimited;
    @Shadow
    @Final
    private List<Predicate<Entity>> contextFreePredicates;
    @Shadow
    @Final
    private MinMaxBounds.Doubles range;
    @Shadow
    @Final
    private Function<Vec3, Vec3> position;
    @Shadow
    @Final
    @Nullable
    private AABB aabb;
    @Shadow
    @Final
    private BiConsumer<Vec3, List<? extends Entity>> order;
    @Shadow
    @Final
    private boolean currentEntity;
    @Shadow
    @Final
    @Nullable
    private String playerName;
    @Shadow
    @Final
    @Nullable
    private UUID entityUUID;
    @Shadow
    @Final
    private EntityTypeTest<Entity, ?> type;
    @Shadow
    @Final
    private boolean usesSelector;

    @Unique
    private void checkPermissionsClient(FabricClientCommandSource source) throws CommandSyntaxException {
    }

    @Shadow
    @Nullable
    private AABB getAbsoluteAabb(Vec3 vec) {
        return null;
    }

    @Shadow
    private Predicate<Entity> getPredicate(Vec3 vec, @Nullable AABB aabb, @Nullable FeatureFlagSet featureFlags) {
        return null;
    }

    @Shadow
    protected abstract void checkPermissions(CommandSourceStack commandSourceStack) throws CommandSyntaxException;

    @Shadow
    private <T extends Entity> List<T> sortAndLimit(Vec3 vec, List<T> list) {
        return null;
    }

    @Override
    public Entity findSingleEntityClient(FabricClientCommandSource source) throws CommandSyntaxException {
        this.checkPermissionsClient(source);
        List<? extends Entity> list = this.findEntitiesClient(source);
        if (list.isEmpty())
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        if (list.size() > 1)
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
        return list.get(0);
    }

    @Override
    public List<? extends Entity> findEntitiesClient(FabricClientCommandSource source) throws CommandSyntaxException {
        this.checkPermissionsClient(source);
        if (!this.includesEntities)
            return this.findPlayersClient(source);
        else if (this.playerName != null) {
            for (Player player : source.getLevel().players())
                if (player.getGameProfile().name().equalsIgnoreCase(playerName))
                    return List.of(player);
            return List.of();
        } else if (this.entityUUID != null) {
            ClientLevel level = (ClientLevel) source.getLevel();
            for (Entity entity : level.entitiesForRendering())
                if (entity.getUUID().equals(entityUUID))
                    return List.of(entity);
            return List.of();
        }

        Vec3 vec3 = this.position.apply(source.getPosition());
        AABB aabb = this.getAbsoluteAabb(vec3);
        Predicate<Entity> predicate = this.getPredicate(vec3, aabb, null);
        if (this.currentEntity)
            return (source.getEntity() != null && predicate.test(source.getEntity()) ? List.of(source.getEntity()) : List.of());
        List<Entity> list = new ObjectArrayList<>();

        ClientLevel level = source.getLevel();

        if (aabb != null)
            list.addAll(level.getEntities(this.type, aabb, predicate));
        else {
            for (Entity entity : level.entitiesForRendering()) {
                if (predicate.test(entity))
                    list.add(entity);
            }
        }
        return this.sortAndLimit(vec3, list);
    }

    @Override
    public Player findSinglePlayerClient(FabricClientCommandSource source) throws CommandSyntaxException {
        this.checkPermissionsClient(source);
        List<Player> list = this.findPlayersClient(source);
        if (list.size() != 1)
            throw EntityArgument.NO_PLAYERS_FOUND.create();
        return list.get(0);
    }

    @Override
    public List<Player> findPlayersClient(FabricClientCommandSource source) throws CommandSyntaxException {
        this.checkPermissionsClient(source);
        if (this.playerName != null) {
            for (Player player : source.getLevel().players())
                if (player.getGameProfile().name().equalsIgnoreCase(playerName))
                    return List.of(player);
            return List.of();
        } else if (this.entityUUID != null) {
            Player player = source.getLevel().getPlayerByUUID(entityUUID);
            return player == null ? List.of() : List.of(player);
        }

        Vec3 vec3 = this.position.apply(source.getPosition());
        AABB aabb = this.getAbsoluteAabb(vec3);
        Predicate<Entity> predicate = this.getPredicate(vec3, aabb, null);
        if (this.currentEntity) {
            if (source.getEntity() instanceof Player player && predicate.test(player))
                return List.of(player);
            return List.of();
        }

        List<Player> list = new ObjectArrayList<>();
        for (Player player : source.getLevel().players())
            if (predicate.test(player))
                list.add(player);

        return this.sortAndLimit(vec3, list);
    }
}