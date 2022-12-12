package moe.plushie.armourers_workshop.compatibility.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import moe.plushie.armourers_workshop.api.math.IMatrix3f;
import moe.plushie.armourers_workshop.api.math.IMatrix4f;
import moe.plushie.armourers_workshop.api.math.IPoseStack;
import moe.plushie.armourers_workshop.api.math.IQuaternionf;
import moe.plushie.armourers_workshop.compatibility.AbstractPoseStack;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PoseStack.class)
@Implements(@Interface(iface = IPoseStack.class, prefix = "aw$"))
public abstract class AbstractPoseStackMixin {

    @Intrinsic(displace = true)
    public void aw$pushPose() {
        _aw$self().pushPose();
    }

    @Intrinsic(displace = true)
    public void aw$popPose() {
        _aw$self().popPose();
    }

    @Intrinsic(displace = true)
    public void aw$translate(float x, float y, float z) {
        _aw$self().translate(x, y, z);
    }

    @Intrinsic(displace = true)
    public void aw$scale(float x, float y, float z) {
        _aw$self().scale(x, y, z);
    }

    @Intrinsic(displace = true)
    public void aw$rotate(IQuaternionf quaternion) {
        _aw$self().mulPose(AbstractPoseStack.of(quaternion));
    }

    @Intrinsic(displace = true)
    public void aw$multiply(IMatrix4f matrix) {
        aw$lastPose().multiply(matrix);
    }

    @Intrinsic(displace = true)
    public IMatrix4f aw$lastPose() {
        return _aw$last().aw$getPose();
    }

    @Intrinsic(displace = true)
    public IMatrix3f aw$lastNormal() {
        return _aw$last().aw$getNormal();
    }

    @Intrinsic(displace = true)
    public PoseStack aw$cast() {
        return _aw$self();
    }

    private PoseStack _aw$self() {
        return ObjectUtils.unsafeCast(this);
    }

    private AbstractPoseStack.Pose _aw$last() {
        return ObjectUtils.unsafeCast(_aw$self().last());
    }
}
