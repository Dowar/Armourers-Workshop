package moe.plushie.armourers_workshop.compatibility.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import moe.plushie.armourers_workshop.api.annotation.Available;
import moe.plushie.armourers_workshop.compatibility.api.AbstractItemTransformType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.neoforge.client.ClientHooks;

@Available("[1.21, )")
public class AbstractForgeClientHooks {

    public static BakedModel handleCameraTransforms(PoseStack poseStack, BakedModel model, AbstractItemTransformType transformType, boolean applyLeftHandTransform) {
        return ClientHooks.handleCameraTransforms(poseStack, model, ItemTransforms.ofType(transformType), applyLeftHandTransform);
    }
}
