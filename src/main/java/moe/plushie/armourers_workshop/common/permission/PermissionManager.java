package moe.plushie.armourers_workshop.common.permission;

import java.util.ArrayList;

import moe.plushie.armourers_workshop.common.blocks.ModBlocks;
import moe.plushie.armourers_workshop.common.items.ModItems;
import moe.plushie.armourers_workshop.utils.ModLogger;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.server.permission.PermissionAPI;

public final class PermissionManager {
    
    public static ArrayList<Permission> getPermissions() {
        ArrayList<Permission> permissions = new ArrayList<Permission>();
        for (Block block : ModBlocks.BLOCK_LIST) {
            if (block instanceof IPermissionHolder) {
                ((IPermissionHolder) block).getPermissions(permissions);
            }
        }
        for (Item item : ModItems.ITEM_LIST) {
            if (item instanceof IPermissionHolder) {
                ((IPermissionHolder) item).getPermissions(permissions);
            }
        }
        return permissions;
    }

    public static void registerPermissions() {
        ArrayList<Permission> permissions = getPermissions();
        for (Permission permission : permissions) {
            ModLogger.log("Registering permission: " + permission.node);
            PermissionAPI.registerNode(permission.node, permission.level, permission.description);
        }
    }
}