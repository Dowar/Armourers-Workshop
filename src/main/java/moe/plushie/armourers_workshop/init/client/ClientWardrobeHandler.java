package moe.plushie.armourers_workshop.init.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import moe.plushie.armourers_workshop.core.entity.MannequinEntity;
import moe.plushie.armourers_workshop.core.render.other.SkinRenderData;
import moe.plushie.armourers_workshop.core.render.skin.SkinRenderer;
import moe.plushie.armourers_workshop.core.render.skin.SkinRendererManager;
import moe.plushie.armourers_workshop.core.skin.SkinDescriptor;
import moe.plushie.armourers_workshop.init.common.ModConfig;
import moe.plushie.armourers_workshop.init.common.ModItems;
import moe.plushie.armourers_workshop.utils.RenderUtils;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.function.Supplier;


@OnlyIn(Dist.CLIENT)
public class ClientWardrobeHandler {

    public final static float SCALE = 1 / 16f;

    public static void init() {
    }

    static Vector3f dx = new Vector3f();

    public static void onRenderTrident(TridentEntity entity, Model model, float p_225623_2_, float partialTicks, int light, MatrixStack matrixStack, IRenderTypeBuffer buffers, CallbackInfo callback) {
        SkinRenderData renderData = SkinRenderData.of(entity);
        if (renderData == null) {
            return;
        }
        matrixStack.pushPose();

        matrixStack.mulPose(Vector3f.YP.rotationDegrees(MathHelper.lerp(partialTicks, entity.yRotO, entity.yRot) - 90.0F));
        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(MathHelper.lerp(partialTicks, entity.xRotO, entity.xRot) + 90.0F));

        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(180));
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(-90));

        matrixStack.scale(-SCALE, -SCALE, SCALE);
        matrixStack.translate(0, 11, 0);

        int count = render(entity, model, light, matrixStack, buffers, ItemCameraTransforms.TransformType.NONE, renderData::getItemSkins);
        if (count != 0) {
            callback.cancel();
        }

        matrixStack.popPose();
    }

    public static void onRenderArrow(AbstractArrowEntity entity, Model model, float p_225623_2_, float partialTicks, int light, MatrixStack matrixStack, IRenderTypeBuffer buffers, CallbackInfo callback) {
        SkinRenderData renderData = SkinRenderData.of(entity);
        if (renderData == null) {
            return;
        }
        matrixStack.pushPose();

        matrixStack.mulPose(Vector3f.YP.rotationDegrees(MathHelper.lerp(partialTicks, entity.yRotO, entity.yRot) - 90.0F));
        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(MathHelper.lerp(partialTicks, entity.xRotO, entity.xRot)));

        float f9 = (float) entity.shakeTime - partialTicks;
        if (f9 > 0.0F) {
            float f10 = -MathHelper.sin(f9 * 3.0F) * f9;
            matrixStack.mulPose(Vector3f.ZP.rotationDegrees(f10));
        }

        matrixStack.mulPose(Vector3f.YP.rotationDegrees(-90));
        matrixStack.scale(-SCALE, -SCALE, SCALE);
        matrixStack.translate(0, 0, -1);

        int count = render(entity, model, light, matrixStack, buffers, ItemCameraTransforms.TransformType.NONE, renderData::getItemSkins);
        if (count != 0) {
            callback.cancel();
        }

        matrixStack.popPose();
    }

//    public static void onRenderArmorPre(Entity entity, EntityModel<?> entityModel, int light, MatrixStack matrixStack, IRenderTypeBuffer buffers) {
//        // apply the model baby scale.
//        if (entityModel.young && entityModel instanceof BipedModel<?>) {
//            BipedModel<?> bipedModel = (BipedModel<?>) entityModel;
//            float scale = 1.0f / bipedModel.babyBodyScale;
//            matrixStack.scale(scale, scale, scale);
//            matrixStack.translate(0.0f, bipedModel.bodyYOffset / 16.0f, 0.0f);
//        }
//    }
//
//    public static void onRenderArmor(Entity entity, Model model, int light, MatrixStack matrixStack, IRenderTypeBuffer buffers) {
//        SkinRenderData renderData = SkinRenderData.of(entity);
//        if (renderData == null) {
//            return;
//        }
//        matrixStack.pushPose();
//        matrixStack.scale(SCALE, SCALE, SCALE);
//
//        render(entity, model, light, matrixStack, buffers, null, renderData::getArmorSkins);
//
//        matrixStack.popPose();
//    }

//    public static void onRenderItem(Entity entity, ItemStack itemStack, ItemCameraTransforms.TransformType transformType, int light, MatrixStack matrixStack, IRenderTypeBuffer buffers, CallbackInfo callback) {
//        SkinRenderData renderData = SkinRenderData.of(entity);
//        if (renderData == null) {
//            return;
//        }
//        matrixStack.pushPose();
//        matrixStack.scale(-SCALE, -SCALE, SCALE);
//
//        boolean replaceSkinItem = entity instanceof MannequinEntity;
//        int count = render(entity, null, light, matrixStack, buffers, transformType, () -> renderData.getItemSkins(itemStack, replaceSkinItem));
//        if (count != 0) {
//            callback.cancel();
//        }
//
//        matrixStack.popPose();
//    }

    public static ItemStack getRenderSkinStack(ItemStack itemStack, boolean isRenderInGUI) {
        if (isRenderInGUI && !ModConfig.Client.enableEmbeddedSkinRenderer) {
            return itemStack;
        }
        if (itemStack.getItem() == ModItems.SKIN) {
            return itemStack;
        }
        SkinDescriptor descriptor = SkinDescriptor.of(itemStack);
        if (!descriptor.isEmpty()) {
            return descriptor.sharedItemStack();
        }
        return itemStack;
    }

    public static void onRenderSkinStack(@Nullable LivingEntity entity, ItemStack itemStack, ItemCameraTransforms.TransformType transformType, boolean p_229109_4_, MatrixStack matrixStack, IRenderTypeBuffer buffers, @Nullable World world, int packedLight, int p_229109_9_, CallbackInfo callback) {
        if (itemStack.isEmpty()) {
            return;
        }
        switch (transformType) {
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND: {
                int count = 0;
                SkinRenderData renderData = SkinRenderData.of(entity);
                if (renderData != null) {
                    matrixStack.pushPose();
                    matrixStack.scale(-SCALE, -SCALE, SCALE);

                    boolean replaceSkinItem = entity instanceof MannequinEntity;
                    count = render(entity, null, packedLight, matrixStack, buffers, transformType, () -> renderData.getItemSkins(itemStack, replaceSkinItem));
                    if (count != 0) {
                        callback.cancel();
                    }
                    matrixStack.popPose();
                }
                break;
            }
        }
    }


    public static void onRenderEntityInInventoryPre(LivingEntity entity, int x, int y, int scale, float mouseX, float mouseY) {
        if (!ModConfig.Client.enableEntityInInventoryClip) {
            return;
        }
        int left, top, width, height;
        switch (scale) {
            case 20: // in creative container screen
                width = 32;
                height = 43;
                left = x - width / 2 + 1;
                top = y - height + 4;
                break;

            case 30: // in survival container screen
                width = 49;
                height = 70;
                left = x - width / 2 - 1;
                top = y - height + 3;
                break;

            default:
                return;
        }
        RenderUtils.enableScissor(left, top, width, height);
    }

    public static void onRenderEntityInInventoryPost(LivingEntity entity) {
        if (!ModConfig.Client.enableEntityInInventoryClip) {
            return;
        }
        RenderUtils.disableScissor();
    }

    public static void onRenderArmorEquipment(LivingEntity entity, Model model, EquipmentSlotType slotType, MatrixStack matrixStack, IRenderTypeBuffer buffers, CallbackInfo callback) {
        SkinRenderer<Entity, Model> renderer = SkinRendererManager.getInstance().getRenderer(entity, model, null);
        if (renderer != null) {
            renderer.apply(entity, model, slotType, 0, matrixStack);
        }
    }

    private static int render(Entity entity, Model model, int light, MatrixStack matrixStack, IRenderTypeBuffer buffers, ItemCameraTransforms.TransformType transformType, Supplier<Iterable<SkinRenderData.Entry>> provider) {
        int r = 0;
        float partialTicks = System.currentTimeMillis() % 100000000;
        SkinRenderer<Entity, Model> renderer = SkinRendererManager.getInstance().getRenderer(entity, model, null);
        if (renderer == null) {
            return 0;
        }
        for (SkinRenderData.Entry entry : provider.get()) {
            renderer.render(entity, model, entry.getBakedSkin(), entry.getBakedScheme(), transformType, light, partialTicks, matrixStack, buffers);
            r += 1;
        }
        return r;
    }
}
