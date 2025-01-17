package moe.plushie.armourers_workshop.compatibility.forge.extensions.net.minecraft.world.item.CreativeModeTab;

import moe.plushie.armourers_workshop.api.annotation.Available;
import moe.plushie.armourers_workshop.init.ModConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.ThisClass;

@Available("[1.20, )")
@Extension
public class BuilderProvider {

    private static ResourceLocation LAST_ITEM_GROUP;

    public static Supplier<CreativeModeTab> createCreativeModeTabFO(@ThisClass Class<?> clazz, String name, Supplier<Supplier<ItemStack>> icon, Consumer<List<ItemStack>> itemProvider) {
        ResourceLocation lastItemGroup = LAST_ITEM_GROUP;
        ResourceLocation registryName = ModConstants.key(name);
        LAST_ITEM_GROUP = registryName;
        return () -> {
            CreativeModeTab.Builder builder = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + registryName.getNamespace() + "." + registryName.getPath()))
                    .icon(() -> icon.get().get())
                    .displayItems((features, output) -> {
                        ArrayList<ItemStack> list = new ArrayList<>();
                        itemProvider.accept(list);
                        output.acceptAll(list);
                    });
            if (lastItemGroup != null) {
                builder = builder.withTabsBefore(lastItemGroup);
            } else {
                builder = builder.withTabsBefore(CreativeModeTabs.SPAWN_EGGS);
            }
            return builder.build();
        };
    }
}
