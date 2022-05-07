package moe.plushie.armourers_workshop.core.skin.part.item;

import moe.plushie.armourers_workshop.api.action.ICanHeld;
import moe.plushie.armourers_workshop.api.skin.property.ISkinProperties;
import moe.plushie.armourers_workshop.core.skin.part.SkinPartType;
import moe.plushie.armourers_workshop.utils.Rectangle3i;
import net.minecraft.util.math.vector.Vector3i;

public class ItemPartType extends SkinPartType implements ICanHeld {

    public ItemPartType() {
        super();
        this.buildingSpace = new Rectangle3i(-10, -20, -28, 20, 62, 56);
        this.guideSpace = new Rectangle3i(-2, -2, 2, 4, 4, 8);
        //Offset -1 to match old skin system.
        this.offset = new Vector3i(0, -1, 0);
    }
}
