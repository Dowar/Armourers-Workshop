package moe.plushie.armourers_workshop.builder.item;

import moe.plushie.armourers_workshop.api.painting.IPaintingToolProperty;
import moe.plushie.armourers_workshop.builder.item.tooloption.ToolOptions;
import moe.plushie.armourers_workshop.core.utils.TranslateUtils;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class PaintRollerItem extends PaintbrushItem {

    public PaintRollerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean applyColor(World worldIn, BlockPos blockPos, Direction direction, ItemStack itemStack, @Nullable PlayerEntity player) {
        int changes = 0;
        int radius = ToolOptions.RADIUS.get(itemStack);
        for (int i = -radius + 1; i < radius; i++ ) {
            for (int j = -radius + 1; j < radius; j++ ) {
                BlockPos targetPos = blockPos;
                switch (direction) {
                    case UP:
                    case DOWN:
                        targetPos = blockPos.offset(j, 0, i);
                        break;
                    case NORTH:
                    case SOUTH:
                        targetPos = blockPos.offset(i, j, 0);
                        break;
                    case WEST:
                    case EAST:
                        targetPos = blockPos.offset(0, i, j);
                        break;
                }
                // TODO: (targetBlock != ModBlocks.BOUNDING_BOX & block != ModBlocks.BOUNDING_BOX) | (targetBlock == ModBlocks.BOUNDING_BOX & block == ModBlocks.BOUNDING_BOX)
                if (super.applyColor(worldIn, targetPos, direction, itemStack, player)) {
                    changes += 1;
                }
            }
        }
        return changes != 0;
    }

    @Override
    public void createToolProperties(Consumer<IPaintingToolProperty<?>> builder) {
        super.createToolProperties(builder);
        builder.accept(ToolOptions.RADIUS);
    }

    @Override
    public void appendSettingHoverText(ItemStack itemStack, List<ITextComponent> tooltips, ITooltipFlag flags) {
        int radius = ToolOptions.RADIUS.get(itemStack);
        tooltips.add(TranslateUtils.subtitle("item.armourers_workshop.rollover.area", radius * 2 - 1, radius * 2 - 1, 1));
        super.appendSettingHoverText(itemStack, tooltips, flags);
    }

//    @Override
//    public void playToolSound(EntityPlayer player, World world, BlockPos pos, ItemStack stack) {
//        world.playSound(null, pos, ModSounds.PAINT, SoundCategory.BLOCKS, 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
//    }
//
//    @Override
//    public void usedOnBlockSide(ItemStack stack, EntityPlayer player, World world, BlockPos pos, Block block, EnumFacing facing, boolean spawnParticles) {
//        boolean fullBlock = false;
//        if (this instanceof IConfigurableTool) {
//            ArrayList<ToolOption<?>> toolOptionList = new ArrayList<ToolOption<?>>();
//            ((IConfigurableTool)this).getToolOptions(toolOptionList);
//            if (toolOptionList.contains(ToolOptions.FULL_BLOCK_MODE)) {
//                fullBlock = ToolOptions.FULL_BLOCK_MODE.getValue(stack);
//            }
//        }
//
//        if (block instanceof IPantableBlock) {
//            int newColour = getToolColour(stack);
//            IPaintType paintType = getToolPaintType(stack);
//            if (!world.isRemote) {
//                IPantableBlock worldColourable = (IPantableBlock) block;
//                if (fullBlock) {
//                    for (int i = 0; i < 6; i++) {
//                        int oldColour = worldColourable.getColour(world, pos, EnumFacing.VALUES[i]);
//                        byte oldPaintType = (byte) worldColourable.getPaintType(world, pos, EnumFacing.VALUES[i]).getId();
//                        UndoManager.blockPainted(player, world, pos, oldColour, oldPaintType, EnumFacing.VALUES[i]);
//                        ((IPantableBlock)block).setColour(world, pos, newColour, EnumFacing.VALUES[i]);
//                        ((IPantableBlock)block).setPaintType(world, pos, paintType, EnumFacing.VALUES[i]);
//                    }
//                } else {
//                    int oldColour = worldColourable.getColour(world, pos, facing);
//                    byte oldPaintType = (byte) worldColourable.getPaintType(world, pos, facing).getId();
//                    UndoManager.blockPainted(player, world, pos, oldColour, oldPaintType, facing);
//                    ((IPantableBlock)block).setColour(world, pos, newColour, facing);
//                    ((IPantableBlock)block).setPaintType(world, pos, paintType, facing);
//                }
//            } else {
//                if (spawnParticles) {
//                    spawnPaintParticles(world, pos, facing, newColour);
//                }
//            }
//        }
//    }

}
