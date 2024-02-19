package moe.plushie.armourers_workshop.compatibility.forge.extensions.net.minecraft.client.renderer.GameRenderer;

import moe.plushie.armourers_workshop.api.annotation.Available;
import moe.plushie.armourers_workshop.api.registry.IRegistryKey;
import moe.plushie.armourers_workshop.compatibility.client.AbstractBlockEntityRendererProvider;
import moe.plushie.armourers_workshop.compatibility.client.AbstractEntityRendererProvider;
import moe.plushie.armourers_workshop.compatibility.forge.AbstractForgeClientEvents;
import moe.plushie.armourers_workshop.compatibility.forge.AbstractForgeEventBus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.ThisClass;

@Available("[1.18, )")
@Extension
public class ForgeRegistry {

    public static <T extends Entity> void registerEntityRendererFO(@ThisClass Class<?> clazz, IRegistryKey<EntityType<T>> entityType, AbstractEntityRendererProvider<T> provider) {
        AbstractForgeEventBus.observer(AbstractForgeClientEvents.ENTITY_RENDERER_REGISTRY, event -> event.registerEntityRenderer(entityType.get(), provider::create));
    }

    public static <T extends BlockEntity> void registerBlockEntityRendererFO(@ThisClass Class<?> clazz, IRegistryKey<BlockEntityType<T>> entityType, AbstractBlockEntityRendererProvider<T> provider) {
        AbstractForgeEventBus.observer(AbstractForgeClientEvents.ENTITY_RENDERER_REGISTRY, event -> event.registerBlockEntityRenderer(entityType.get(), provider::create));
    }
}
