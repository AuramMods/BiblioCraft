package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.DisplayCaseBlock;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public class DisplayCaseBlockEntity extends FurnitureInventoryBlockEntity {
    public static final ModelProperty<DyeColor> LINING_COLOR = new ModelProperty<>();
    public static final int DISPLAY_SLOT = 0;
    public static final int LINING_SLOT = 1;
    private boolean powered;

    public DisplayCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CASE.get(), pos, state, 2);
    }

    public static boolean isLining(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof WoolCarpetBlock;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == DISPLAY_SLOT || (getItem(LINING_SLOT).isEmpty() && isLining(stack));
    }

    @Override
    protected int getSlotLimit(int slot) {
        return slot == LINING_SLOT ? 1 : super.getSlotLimit(slot);
    }

    public DyeColor getLiningColor() {
        ItemStack lining = getItem(LINING_SLOT);
        return lining.getItem() instanceof BlockItem item && item.getBlock() instanceof WoolCarpetBlock carpet
                ? carpet.getColor() : DyeColor.WHITE;
    }

    public boolean hasRedstoneBlock() {
        return getItem(DISPLAY_SLOT).is(Items.REDSTONE_BLOCK);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && getBlockState().getValue(DisplayCaseBlock.OPEN);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            boolean hasRedstone = hasRedstoneBlock();
            if (powered != hasRedstone) {
                powered = hasRedstone;
                DisplayCaseBlock.updateSignalNeighbors(level, worldPosition, getBlockState());
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        DyeColor previous = getLiningColor();
        super.load(tag);
        if (level == null) {
            powered = hasRedstoneBlock();
        }
        if (getLiningColor() != previous) {
            refreshModel();
        }
    }

    @Override
    public ModelData getModelData() {
        return super.getModelData().derive().with(LINING_COLOR, getLiningColor()).build();
    }
}
