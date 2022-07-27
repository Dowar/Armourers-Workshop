package moe.plushie.armourers_workshop.core.item;

import moe.plushie.armourers_workshop.core.data.MannequinHitResult;
import moe.plushie.armourers_workshop.core.entity.MannequinEntity;
import moe.plushie.armourers_workshop.core.texture.PlayerTextureDescriptor;
import moe.plushie.armourers_workshop.init.ModEntities;
import moe.plushie.armourers_workshop.init.ModItems;
import moe.plushie.armourers_workshop.utils.Constants;
import moe.plushie.armourers_workshop.utils.TranslateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MannequinItem extends FlavouredItem {

    public MannequinItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(@Nullable Player player, float scale) {
        ItemStack itemStack = new ItemStack(ModItems.MANNEQUIN.get());
        CompoundTag entityTag = new CompoundTag();
        if (scale != 1.0f) {
            entityTag.putFloat(Constants.Key.ENTITY_SCALE, scale);
        }
        if (player != null) {
            PlayerTextureDescriptor descriptor = new PlayerTextureDescriptor(player.getGameProfile());
            entityTag.put(Constants.Key.ENTITY_TEXTURE, descriptor.serializeNBT());
        }
        if (entityTag.size() != 0) {
            CompoundTag nbt = itemStack.getOrCreateTag();
            nbt.put(Constants.Key.ENTITY, entityTag);
        }
        return itemStack;

    }

    public static boolean isSmall(ItemStack itemStack) {
        CompoundTag entityTag = itemStack.getTagElement(Constants.Key.ENTITY);
        if (entityTag != null) {
            return entityTag.getBoolean(Constants.Key.ENTITY_IS_SMALL);
        }
        return false;
    }

    public static void setScale(ItemStack itemStack, float scale) {
        CompoundTag entityTag = itemStack.getOrCreateTagElement(Constants.Key.ENTITY);
        entityTag.putFloat(Constants.Key.ENTITY_SCALE, scale);
    }

    public static float getScale(ItemStack itemStack) {
        CompoundTag entityTag = itemStack.getTagElement(Constants.Key.ENTITY);
        if (entityTag == null || !entityTag.contains(Constants.Key.ENTITY_SCALE, Constants.TagFlags.FLOAT)) {
            return 1.0f;
        }
        return entityTag.getFloat(Constants.Key.ENTITY_SCALE);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        Level world = context.getLevel();
        Vec3 origin = new Vec3(player.getX(), player.getY(), player.getZ());
        MannequinHitResult rayTraceResult = MannequinHitResult.test(player, origin, context.getClickLocation(), context.getClickedPos());
        ItemStack itemStack = context.getItemInHand();
        if (world instanceof ServerLevel) {
            ServerLevel serverWorld = (ServerLevel) world;
            MannequinEntity entity = ModEntities.MANNEQUIN.get().create(serverWorld, itemStack.getTag(), null, context.getPlayer(), rayTraceResult.getBlockPos(), MobSpawnType.SPAWN_EGG, true, true);
            if (entity == null) {
                return InteractionResult.FAIL;
            }
            Vec3 clickedLocation = rayTraceResult.getLocation();
            entity.absMoveTo(clickedLocation.x(), clickedLocation.y(), clickedLocation.z(), 0.0f, 0.0f);
            entity.setYBodyRot(rayTraceResult.getRotation());

            world.addFreshEntity(entity);
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);

            itemStack.shrink(1);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.FAIL;
    }

    @Override
    public String getDescriptionId(ItemStack itemStack) {
        float scale = getScale(itemStack);
        if (scale <= 0.5f) {
            return super.getDescriptionId(itemStack) + ".small";
        }
        if (scale >= 2.0f) {
            return super.getDescriptionId(itemStack) + ".big";
        }
        return super.getDescriptionId(itemStack);
    }

    @Override
    @Environment(value = EnvType.CLIENT)
    public void appendHoverText(ItemStack itemStack, @Nullable Level world, List<Component> tooltips, TooltipFlag flag) {
        super.appendHoverText(itemStack, world, tooltips, flag);
        PlayerTextureDescriptor descriptor = PlayerTextureDescriptor.of(itemStack);
        if (descriptor.getName() != null) {
            tooltips.add(TranslateUtils.subtitle("item.armourers_workshop.rollover.user", descriptor.getName()));
        }
        if (descriptor.getURL() != null) {
            tooltips.add(TranslateUtils.subtitle("item.armourers_workshop.rollover.url", descriptor.getURL()));
        }
    }
}