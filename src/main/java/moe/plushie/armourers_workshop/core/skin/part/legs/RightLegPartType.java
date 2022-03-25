package moe.plushie.armourers_workshop.core.skin.part.legs;

import moe.plushie.armourers_workshop.api.skin.ISkinPartTypeTextured;
import moe.plushie.armourers_workshop.api.skin.ISkinProperties;
import moe.plushie.armourers_workshop.core.skin.data.property.SkinProperty;
import moe.plushie.armourers_workshop.core.skin.part.SkinPartType;
import moe.plushie.armourers_workshop.core.utils.Rectangle3i;
import net.minecraft.util.math.vector.Vector3i;

import java.awt.*;

public class RightLegPartType extends SkinPartType implements ISkinPartTypeTextured {

    public RightLegPartType() {
        super();
        this.buildingSpace = new Rectangle3i(-3, -8, -8, 11, 9, 16);
        this.guideSpace = new Rectangle3i(-2, -12, -2, 4, 12, 4);
        this.offset = new Vector3i(-6, -5, 0);
    }

    @Override
    public void renderBuildingGuide(float scale, ISkinProperties skinProps, boolean showHelper) {
//        GL11.glTranslated(0, this.buildingSpace.getY() * scale, 0);
//        GL11.glTranslated(0, -this.guideSpace.getY() * scale, 0);
//        ModelLegs.MODEL.renderRightLeg(scale);
//        GL11.glTranslated(0, this.guideSpace.getY() * scale, 0);
//        GL11.glTranslated(0, -this.buildingSpace.getY() * scale, 0);
    }

    @Override
    public Point getTextureSkinPos() {
        return new Point(0, 16);
    }

    @Override
    public boolean isTextureMirrored() {
        return false;
    }

    @Override
    public Point getTextureBasePos() {
        return new Point(0, 16);
    }

    @Override
    public Point getTextureOverlayPos() {
        return new Point(0, 32);
    }

    @Override
    public Vector3i getTextureModelSize() {
        return new Vector3i(4, 12, 4);
    }

    @Override
    public Vector3i getRenderOffset() {
        return new Vector3i(-2, 12, 0);
    }

    @Override
    public float getRenderPolygonOffset() {
        return -0.02f;
    }

    @Override
    public boolean isModelOverridden(ISkinProperties skinProps) {
        return skinProps.get(SkinProperty.MODEL_OVERRIDE_LEG_RIGHT);
    }

    @Override
    public boolean isOverlayOverridden(ISkinProperties skinProps) {
        return skinProps.get(SkinProperty.MODEL_HIDE_OVERLAY_LEG_RIGHT);
    }
}
