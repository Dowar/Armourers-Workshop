package moe.plushie.armourers_workshop.compatibility.fabric;

import moe.plushie.armourers_workshop.api.annotation.Available;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

@Available("[1.18, )")
public interface AbstractFabricMenuType {

    static <T extends AbstractContainerMenu> MenuType<T> create(ExtendedScreenHandlerType.ExtendedFactory<T> factory) {
        return new ExtendedScreenHandlerType<>(factory);
    }
}
