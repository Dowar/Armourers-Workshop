package moe.plushie.armourers_workshop.core.client.render;

import com.apple.library.uikit.UIColor;
import com.mojang.blaze3d.vertex.PoseStack;
import moe.plushie.armourers_workshop.core.client.bake.BakedSkin;
import moe.plushie.armourers_workshop.core.client.other.PlaceholderManager;
import moe.plushie.armourers_workshop.core.client.other.SkinItemSource;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderData;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderTesselator;
import moe.plushie.armourers_workshop.core.data.color.ColorScheme;
import moe.plushie.armourers_workshop.core.entity.MannequinEntity;
import moe.plushie.armourers_workshop.core.texture.PlayerTextureDescriptor;
import moe.plushie.armourers_workshop.init.ModDebugger;
import moe.plushie.armourers_workshop.utils.RenderSystem;
import moe.plushie.armourers_workshop.utils.ShapeTesselator;
import moe.plushie.armourers_workshop.utils.TickUtils;
import moe.plushie.armourers_workshop.utils.math.OpenMatrix3f;
import moe.plushie.armourers_workshop.utils.math.OpenMatrix4f;
import moe.plushie.armourers_workshop.utils.math.OpenQuaternionf;
import moe.plushie.armourers_workshop.utils.math.Rectangle3f;
import moe.plushie.armourers_workshop.utils.math.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import manifold.ext.rt.api.auto;

@Environment(EnvType.CLIENT)
public final class ExtendedItemRenderer {

    public static void renderSkinInGUI(BakedSkin bakedSkin, float x, float y, float z, float width, float height, float rx, float ry, float rz, PoseStack poseStack, MultiBufferSource buffers) {
        renderSkinInBox(bakedSkin, ColorScheme.EMPTY, ItemStack.EMPTY, getTarget(bakedSkin), x, y, z, width, height, rx, ry, rz, 0, 0xf000f0, poseStack, buffers);
    }

    public static void renderSkinInGUI(BakedSkin bakedSkin, ColorScheme scheme, ItemStack itemStack, float x, float y, float z, float width, float height, float rx, float ry, float rz, float partialTicks, int light, PoseStack poseStack, MultiBufferSource buffers) {
        renderSkinInBox(bakedSkin, scheme, itemStack, null, x, y, z, width, height, rx, ry, rz, partialTicks, light, poseStack, buffers);
    }

    public static void renderSkinInTooltip(BakedSkin bakedSkin, ColorScheme scheme, ItemStack itemStack, float x, float y, float z, float width, float height, float rx, float ry, float rz, float partialTicks, int light, PoseStack poseStack, MultiBufferSource buffers) {
        renderSkinInBox(bakedSkin, scheme, itemStack, Vector3f.ONE, x, y, z, width, height, rx, ry, rz, partialTicks, light, poseStack, buffers);
    }

    public static int renderSkinInBox(BakedSkin bakedSkin, ColorScheme scheme, Vector3f scale, float partialTicks, int light, SkinItemSource itemSource, PoseStack poseStack, MultiBufferSource buffers) {
        return renderSkinInBox(bakedSkin, scheme, scale, getTarget(bakedSkin), partialTicks, light, itemSource, poseStack, buffers);
    }

    private static void renderSkinInBox(BakedSkin bakedSkin, ColorScheme scheme, ItemStack itemStack, @Nullable Vector3f target, float x, float y, float z, float width, float height, float rx, float ry, float rz, float partialTicks, int light, PoseStack poseStack, MultiBufferSource buffers) {
        if (bakedSkin != null) {
            int t = TickUtils.ticks();
            float si = Math.min(width, height);
            poseStack.pushPose();
            poseStack.translate(x + width / 2f, y + height / 2f, z);
            poseStack.mulPoseMatrix(OpenMatrix4f.createScaleMatrix(1, -1, 1));
            poseStack.mulNormalMatrix(OpenMatrix3f.createScaleMatrix(1, -1, 1));
            poseStack.mulPose(Vector3f.XP.rotationDegrees(rx));
            poseStack.mulPose(Vector3f.YP.rotationDegrees(ry + (float) (t / 10 % 360)));
            poseStack.scale(0.625f, 0.625f, 0.625f);
            poseStack.scale(si, si, si);
            renderSkinInBox(bakedSkin, scheme, Vector3f.ONE, target, partialTicks, light, SkinItemSource.create(itemStack), poseStack, buffers);
            poseStack.popPose();
        }
    }

    private static int renderSkinInBox(BakedSkin bakedSkin, ColorScheme scheme, Vector3f scale, @Nullable Vector3f target, float partialTicks, int light, SkinItemSource itemSource, PoseStack poseStack, MultiBufferSource buffers) {
        int counter = 0;
        SkinRenderTesselator context = SkinRenderTesselator.create(bakedSkin);
        if (context == null) {
            return counter;
        }
        poseStack.pushPose();
        poseStack.scale(-1, -1, 1);

        context.setLightmap(light);
        context.setPartialTicks(partialTicks);
        context.setRenderData(SkinRenderData.of(context.getMannequin()));
        context.setColorScheme(scheme);
        context.setReferenced(itemSource);

        // ...
        if (target != null) {
            Rectangle3f rect = context.getBakedRenderBounds();
            float targetWidth = target.getX();
            float targetHeight = target.getY();
            float targetDepth = target.getZ();
            float newScale = Math.min(targetWidth / rect.getWidth(), targetHeight / rect.getHeight());
            newScale = Math.min(newScale, targetDepth / rect.getDepth());
            if (ModDebugger.targetBounds) {
                ShapeTesselator.stroke(-targetWidth / 2, -targetHeight / 2, -targetDepth / 2, targetWidth / 2, targetHeight / 2, targetDepth / 2, UIColor.ORANGE, poseStack, buffers);
                ShapeTesselator.vector(0, 0, 0, targetWidth, targetHeight, targetDepth, poseStack, buffers);
            }
            poseStack.scale(newScale / scale.getX(), newScale / scale.getY(), newScale / scale.getZ());
            poseStack.translate(-rect.getMidX(), -rect.getMidY(), -rect.getMidZ()); // to model center
        } else {
            float newScale = 1 / 16f;
            poseStack.scale(newScale, newScale, newScale);
        }

        counter = context.draw(poseStack, buffers);

        poseStack.popPose();

        return counter;
    }

    public static void renderMannequin(PlayerTextureDescriptor descriptor, Vector3f rotation, Vector3f scale, float targetWidth, float targetHeight, float targetDepth, float partialTicks, int light, PoseStack poseStack, MultiBufferSource buffers) {
        MannequinEntity entity = PlaceholderManager.MANNEQUIN.get();
        if (entity == null || entity.getLevel() == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180));

        if (!descriptor.equals(entity.getTextureDescriptor())) {
            entity.setTextureDescriptor(descriptor);
        }

        Rectangle3f rect = new Rectangle3f(entity.getBoundingBox());
        if (ModDebugger.targetBounds) {
            ShapeTesselator.stroke(-targetWidth / 2, -targetHeight / 2, -targetDepth / 2, targetWidth / 2, targetHeight / 2, targetDepth / 2, UIColor.ORANGE, poseStack, buffers);
            ShapeTesselator.vector(0, 0, 0, targetWidth, targetHeight, targetDepth, poseStack, buffers);
        }

        Rectangle3f resolvedRect = rect.offset(rect.getMidX(), rect.getMidY(), rect.getMidZ());
        resolvedRect.mul(new OpenMatrix4f(new OpenQuaternionf(rotation.getX(), rotation.getY(), rotation.getZ(), true)));
        float newScale = Math.min(targetWidth / resolvedRect.getWidth(), targetHeight / resolvedRect.getHeight());

        poseStack.scale(newScale, newScale, newScale);
        poseStack.translate(-rect.getMidX(), -rect.getMidY(), -rect.getMidZ()); // to model center

        auto rendererManager = Minecraft.getInstance().getEntityRenderDispatcher();
        RenderSystem.runAsFancy(() -> rendererManager.render(entity, 0.0d, 0.0d, 0.0d, 0.0f, 1.0f, poseStack, buffers, light));

        poseStack.popPose();
    }

    private static Vector3f getTarget(BakedSkin bakedSkin) {
        // when no provided a item model, we will use the default target.
        if (bakedSkin != null && bakedSkin.getItemModel() == null) {
            return Vector3f.ONE;
        }
        return null;
    }
}

