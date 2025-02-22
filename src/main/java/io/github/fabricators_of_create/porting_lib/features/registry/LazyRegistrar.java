package io.github.fabricators_of_create.porting_lib.features.registry;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public class LazyRegistrar<T> {
    @Deprecated
    public final String mod_id;
    public final String modId;
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final Map<RegistryObject<T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    private final Set<RegistryObject<T>> entriesView = Collections.unmodifiableSet(entries.keySet());
    private Supplier<Registry<T>> cachedHolder;

    LazyRegistrar(ResourceKey<? extends Registry<T>> registryKey, String modid) {
        this.registryKey = registryKey;
        this.modId = mod_id = modid;
    }

    public static <R> LazyRegistrar<R> create(Registry<R> registry, String id) {
        return new LazyRegistrar<>(registry.key(), id);
    }

    public static <B> LazyRegistrar<B> create(ResourceKey<? extends Registry<B>> registry, String id) {
        return new LazyRegistrar<>(registry, id);
    }

    public static <B> LazyRegistrar<B> create(ResourceLocation registryName, String id) {
        return new LazyRegistrar<>(ResourceKey.createRegistryKey(registryName), id);
    }

    public Supplier<Registry<T>> makeRegistry() {
        if (cachedHolder == null)
            cachedHolder = new RegistryHolder<>(getRegistryKey());
        return cachedHolder;
    }

    public <R extends T> RegistryObject<R> register(String id, Supplier<? extends R> entry) {
        return register(new ResourceLocation(modId, id), entry);
    }

    public <R extends T> RegistryObject<R> register(ResourceLocation id, final Supplier<? extends R> entry) {
        RegistryObject<R> obj = new RegistryObject<>(id, ResourceKey.create(getRegistryKey(), id));
        obj.setGetter(entry);
        if (entries.putIfAbsent((RegistryObject<T>) obj, entry) != null) {
            throw new IllegalArgumentException("Duplicate registration " + id);
        }
        return obj;
    }

    public void register() {
        Registry<T> registry = makeRegistry().get();
        entries.forEach((entry, sup) -> {
            Registry.register(registry, entry.getId(), entry.get());
        });
    }

    public <B extends Block> RegistryObject<T> register(String name, T b) {
        return register(name, () -> b);
    }

    /**
     * Creates a tag key based on the current modId and provided path as the location and the registry name linked to this DeferredRegister.
     * To control the namespace, use {@link #createTagKey(ResourceLocation)}.
     *
     * @throws IllegalStateException If the registry name was not set.
     *                               Use the factories that take {@link #create(ResourceLocation, String) a registry name}}.
     * @see #createTagKey(ResourceLocation)
     */
    @NotNull
    public TagKey<T> createTagKey(@NotNull String path) {
        Objects.requireNonNull(path);
        return createTagKey(new ResourceLocation(this.modId, path));
    }

    /**
     * Creates a tag key based on the provided resource location and the registry name linked to this DeferredRegister.
     * To use the current modid as the namespace, use {@link #createTagKey(String)}.
     *
     * @throws IllegalStateException If the registry name was not set.
     *                               Use the factories that take {@link #create(ResourceLocation, String) a registry name}}.
     * @see #createTagKey(String)
     */
    @NotNull
    public TagKey<T> createTagKey(@NotNull ResourceLocation location) {
        if (this.registryKey == null)
            throw new IllegalStateException("The registry name was not set, cannot create a tag key");
        Objects.requireNonNull(location);
        return TagKey.create(this.registryKey, location);
    }

    public Collection<RegistryObject<T>> getEntries() {
        return entriesView;
    }

    /**
     * @return The registry key stored in this deferred register. Useful for creating new deferred registers based on an existing one.
     */
    public ResourceKey<? extends Registry<T>> getRegistryKey() {
        return this.registryKey;
    }

    private static class RegistryHolder<V> implements Supplier<Registry<V>> {
        private final ResourceKey<? extends Registry<V>> registryKey;
        private final List<Registry<? extends Registry<?>>> registries;
        private Registry<V> registry = null;

        private RegistryHolder(ResourceKey<? extends Registry<V>> registryKey) {
            this.registryKey = registryKey;
            this.registries = new ArrayList<>();
            registerRegistry(BuiltInRegistries.REGISTRY);
//			registerRegistry(BuiltinRegistries.REGISTRY);
        }

        public void registerRegistry(Registry<? extends Registry<?>> registry) {
            registries.add(registry);
        }

        @Override
        public Registry<V> get() {
            // Keep looking up the registry until it's not null
            registries.forEach(reg -> {
                if (reg.containsKey(registryKey.location()))
                    this.registry = (Registry<V>) reg.get(registryKey.location());
            });
            if (this.registry == null)
                this.registry = (Registry<V>) FabricRegistryBuilder.createSimple(null, registryKey.location()).buildAndRegister();

            return this.registry;
        }
    }
}
