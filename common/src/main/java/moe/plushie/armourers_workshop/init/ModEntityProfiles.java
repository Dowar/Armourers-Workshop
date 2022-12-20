package moe.plushie.armourers_workshop.init;

import moe.plushie.armourers_workshop.api.data.IDataPackBuilder;
import moe.plushie.armourers_workshop.api.data.IDataPackObject;
import moe.plushie.armourers_workshop.api.skin.ISkinType;
import moe.plushie.armourers_workshop.core.data.DataPackLoader;
import moe.plushie.armourers_workshop.core.entity.EntityProfile;
import moe.plushie.armourers_workshop.core.skin.SkinTypes;
import moe.plushie.armourers_workshop.init.platform.DataPackManager;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import moe.plushie.armourers_workshop.utils.SkinFileUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public class ModEntityProfiles {

    private static final ArrayList<BiConsumer<EntityType<?>, EntityProfile>> INSERT_HANDLERS = new ArrayList<>();
    private static final ArrayList<BiConsumer<EntityType<?>, EntityProfile>> REMOVE_HANDLERS = new ArrayList<>();

    private static final HashMap<ResourceLocation, EntityProfile> PENDING_ENTITY_PROFILES = new HashMap<>();
    private static final HashMap<EntityType<?>, EntityProfile> PENDING_ENTITIES = new HashMap<>();

    private static final HashMap<ResourceLocation, EntityProfile> ALL_ENTITY_PROFILES = new HashMap<>();
    private static final HashMap<EntityType<?>, EntityProfile> ALL_ENTITIES = new HashMap<>();

    private static void clean() {
    }

    private static void freeze() {
        //
        ObjectUtils.difference(ALL_ENTITY_PROFILES, PENDING_ENTITY_PROFILES, (registryName, entityProfile) -> {
            ModLog.debug("Unregistering Entity Profile '{}'", registryName);
        }, (registryName, entityProfile) -> {
            ModLog.debug("Registering Entity Profile '{}'", registryName);
        });
        ObjectUtils.difference(ALL_ENTITIES, PENDING_ENTITIES, dispatch(REMOVE_HANDLERS), dispatch(INSERT_HANDLERS));
        // apply changes
        ALL_ENTITIES.clear();
        ALL_ENTITY_PROFILES.clear();
        ALL_ENTITIES.putAll(PENDING_ENTITIES);
        ALL_ENTITY_PROFILES.putAll(PENDING_ENTITY_PROFILES);
        PENDING_ENTITIES.clear();
        PENDING_ENTITY_PROFILES.clear();
    }

    private static BiConsumer<EntityType<?>, EntityProfile> dispatch(ArrayList<BiConsumer<EntityType<?>, EntityProfile>> consumers) {
        return (entityType, entityProfile) -> consumers.forEach(consumer -> consumer.accept(entityType, entityProfile));
    }

    public static void init() {
        DataPackManager.register(new DataPackLoader("skin/profiles", SimpleLoader::new, ModEntityProfiles::clean, ModEntityProfiles::freeze));
    }

    public static void forEach(BiConsumer<EntityType<?>, EntityProfile> consumer) {
        ALL_ENTITIES.forEach(consumer);
    }

    public static void addListener(BiConsumer<EntityType<?>, EntityProfile> removeHandler, BiConsumer<EntityType<?>, EntityProfile> insertHandler) {
        REMOVE_HANDLERS.add(removeHandler);
        INSERT_HANDLERS.add(insertHandler);
    }

    @Nullable
    public static <T extends Entity> EntityProfile getProfile(T entity) {
        return getProfile(entity.getType());
    }

    @Nullable
    public static <T extends Entity> EntityProfile getProfile(EntityType<T> entityType) {
        return ALL_ENTITIES.get(entityType);
    }

    @Nullable
    public static EntityProfile getProfile(ResourceLocation registryName) {
        return ALL_ENTITY_PROFILES.get(registryName);
    }

    public static class SimpleLoader implements IDataPackBuilder {

        private boolean locked = false;

        private final ResourceLocation registryName;

        private final ArrayList<EntityType<?>> entities = new ArrayList<>();
        private final HashMap<ISkinType, Function<ISkinType, Integer>> supports = new HashMap<>();

        public SimpleLoader(ResourceLocation registryName) {
            this.registryName = ModConstants.key(SkinFileUtils.getBaseName(registryName.getPath()));
        }

        @Override
        public void append(IDataPackObject object, ResourceLocation location) {
            if (object.get("replace").boolValue()) {
                locked = false;
                supports.clear();
                entities.clear();
            }
            object.get("locked").ifPresent(o -> {
                locked = o.boolValue();
            });
            object.get("slots").entrySet().forEach(it -> {
                ISkinType type = SkinTypes.byName(it.getKey());
                String name = it.getValue().stringValue();
                if (name.equals("default_mob_slots")) {
                    supports.put(type, type1 -> ModConfig.Common.prefersWardrobeMobSlots);
                } else if (name.equals("default_player_slots")) {
                    supports.put(type, type1 -> ModConfig.Common.prefersWardrobePlayerSlots);
                } else {
                    int count = it.getValue().numberValue().intValue();
                    supports.put(type, type1 -> count);
                }
            });
            object.get("entities").allValues().forEach(o -> {
                Optional<EntityType<?>> entityType = EntityType.byString(o.stringValue());
                entityType.ifPresent(entities::add);
            });
        }

        @Override
        public void build() {
            EntityProfile profile = new EntityProfile(registryName, supports, entities, locked);
            entities.forEach(entityType -> PENDING_ENTITIES.put(entityType, profile));
            PENDING_ENTITY_PROFILES.put(registryName, profile);
        }
    }
}
