package moe.plushie.armourers_workshop.core.render.skin;

import moe.plushie.armourers_workshop.api.skin.ISkinDataProvider;
import moe.plushie.armourers_workshop.core.entity.EntityProfile;
import moe.plushie.armourers_workshop.core.entity.EntityProfiles;
import moe.plushie.armourers_workshop.core.render.layer.SkinWardrobeLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.*;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class SkinRendererManager {

    private static final SkinRendererManager INSTANCE = new SkinRendererManager();

    private EntityType<?> lastEntityType;
    private SkinRenderer<?, ?> lastSkinRenderer;

    public static void init() {
        SkinRendererManager skinRendererManager = getInstance();
        EntityRendererManager entityRenderManager = Minecraft.getInstance().getEntityRenderDispatcher();
        for (PlayerRenderer playerRenderer : entityRenderManager.getSkinMap().values()) {
            skinRendererManager.setupRenderer(EntityType.PLAYER, playerRenderer, true);
        }
        entityRenderManager.renderers.forEach((entityType1, entityRenderer) -> {
            if (entityRenderer instanceof LivingRenderer<?, ?>) {
                skinRendererManager.setupRenderer(entityType1, (LivingRenderer<?, ?>) entityRenderer, true);
            }
        });
    }

    public static SkinRendererManager getInstance() {
        return INSTANCE;
    }

    public <T extends Entity, M extends Model> void register(EntityType<T> entityType, EntityProfile entityProfile) {
        EntityRendererManager entityRenderManager = Minecraft.getInstance().getEntityRenderDispatcher();
        if (entityProfile == null) {
            return;
        }
        // Add our own custom armor layer to the various player renderers.
        if (entityType == EntityType.PLAYER) {
            for (PlayerRenderer playerRenderer : entityRenderManager.getSkinMap().values()) {
                setupRenderer(entityType, playerRenderer, false);
            }
        }
        // Add our own custom armor layer to everything that has an armor layer
        entityRenderManager.renderers.forEach((entityType1, entityRenderer) -> {
            if (entityType.equals(entityType1)) {
                if (entityRenderer instanceof LivingRenderer<?, ?>) {
                    setupRenderer(entityType, (LivingRenderer<?, ?>) entityRenderer, false);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Entity, M extends Model> SkinRenderer<T, M> getRenderer(@Nullable T entity) {
        if (entity == null) {
            return null;
        }
        EntityType<?> entityType = entity.getType();
        if (lastEntityType != null && lastEntityType.equals(entityType)) {
            return (SkinRenderer<T, M>) lastSkinRenderer;
        }
        EntityRenderer<? super T> entityRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        SkinRenderer<T, M> skinRenderer = getRenderer(entity.getType(), entityRenderer);
        if (skinRenderer != null) {
            lastEntityType = entityType;
            lastSkinRenderer = skinRenderer;
            return skinRenderer;
        }
        return null;
    }

    @Nullable
    protected <T extends Entity, M extends Model> SkinRenderer<T, M> getRenderer(EntityType<?> entityType, EntityRenderer<? super T> entityRenderer) {
        ISkinDataProvider dataProvider = (ISkinDataProvider) entityRenderer;
        Optional<SkinRenderer<T, M>> skinRenderer = dataProvider.getSkinData();
        if (skinRenderer == null) {
            skinRenderer = Optional.ofNullable(createRenderer(entityType, entityRenderer));
            skinRenderer.ifPresent(SkinRenderer::initTransformers);
            dataProvider.setSkinData(skinRenderer);
        }
        return skinRenderer.orElse(null);
    }


    @Nullable
    protected <T extends Entity, M extends Model> SkinRenderer<T, M> createRenderer(EntityType<?> entityType, EntityRenderer<? super T> entityRenderer) {
        //
        EntityProfile entityProfile = EntityProfiles.getProfile(entityType);

        // using special skin renderer of the arrow.
        if (entityRenderer instanceof ArrowRenderer) {
            return createRenderer(entityProfile, entityRenderer, ArrowSkinRenderer::new);
        }

        EntityModel<?> entityModel = getModel(entityRenderer);
        if (entityModel instanceof IllagerModel) {
            return createRenderer(entityProfile, entityRenderer, IllagerSkinRenderer::new);
        }

        if (entityModel instanceof ZombieVillagerModel) {
            return createRenderer(entityProfile, entityRenderer, ZombieVillagerSkinRenderer::new);
        }
        if (entityModel instanceof VillagerModel) {
            return createRenderer(entityProfile, entityRenderer, VillagerSkinRenderer::new);
        }

        if (entityModel instanceof PlayerModel) {
            return createRenderer(entityProfile, entityRenderer, PlayerSkinRenderer::new);
        }
        if (entityModel instanceof BipedModel) {
            return createRenderer(entityProfile, entityRenderer, BipedSkinRenderer::new);
        }

        if (entityModel instanceof SlimeModel) {
            return createRenderer(entityProfile, entityRenderer, SlimeSkinRenderer::new);
        }

        return null;
    }


    protected EntityModel<?> getModel(EntityRenderer<?> entityRenderer) {
        if (entityRenderer instanceof IEntityRenderer) {
            return ((IEntityRenderer<?, ?>) entityRenderer).getModel();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Entity, M extends Model, T1 extends Entity, M1 extends Model> SkinRenderer<T1, M1> createRenderer(EntityProfile entityProfile, EntityRenderer<?> entityRenderer, Function<EntityProfile, SkinRenderer<T, M>> builder) {
        SkinRenderer<T, M> skinRenderer = builder.apply(entityProfile);
        skinRenderer.initTransformers();
        skinRenderer.init((EntityRenderer<T>) entityRenderer);
        return (SkinRenderer<T1, M1>) skinRenderer;
    }


    private <T extends LivingEntity, M extends EntityModel<T>> void setupRenderer(EntityType<?> entityType, LivingRenderer<T, M> livingRenderer, boolean autoInject) {
        LayerRenderer<T, M> armorLayer = null;
        for (LayerRenderer<T, M> layerRenderer : livingRenderer.layers) {
            if (layerRenderer instanceof BipedArmorLayer<?, ?, ?>) {
                armorLayer = layerRenderer;
            }
            if (layerRenderer instanceof SkinWardrobeLayer) {
                break; // ignore, only one.
            }
        }
        if (autoInject && armorLayer == null) {
            return;
        }
        SkinRenderer<T, M> skinRenderer = getRenderer(entityType, livingRenderer);
        livingRenderer.addLayer(new SkinWardrobeLayer<>(skinRenderer, livingRenderer));
    }
}
