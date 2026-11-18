package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.block.FurniturePanelerBlock;
import art.arcane.bibliocraft.item.FurnitureBlockItem;
import art.arcane.bibliocraft.item.SeatBackItem;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nullable;
import java.util.Objects;

public class FurniturePanelerBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int PANELS = 0;
    public static final int FURNITURE = 1;
    public static final int SLOT_COUNT = 2;
    public static final ModelProperty<ResourceLocation> PANEL_SOURCE = new ModelProperty<>();

    public FurniturePanelerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNITURE_PANELER.get(), pos, state, SLOT_COUNT);
    }

    public static boolean acceptsPanel(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item
                && item.getBlock().defaultBlockState().isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    public static boolean acceptsFurniture(ItemStack stack) {
        return (stack.getItem() instanceof FurnitureBlockItem || stack.getItem() instanceof SeatBackItem)
                && FurnitureMaterial.fromItem(stack).wood() == FurnitureMaterial.Wood.FRAMED;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == PANELS ? acceptsPanel(stack) : acceptsFurniture(stack);
    }

    public ItemStack getResult() {
        ResourceLocation panel = getPanelSource();
        ItemStack furniture = getItem(FURNITURE);
        if (panel == null || !acceptsFurniture(furniture)
                || panel.equals(FurnitureMaterial.fromItem(furniture).panel())) {
            return ItemStack.EMPTY;
        }
        return new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, panel).applyTo(furniture.copyWithCount(1));
    }

    public void consumeInputs() {
        getItem(PANELS).shrink(1);
        getItem(FURNITURE).shrink(1);
        setChanged();
    }

    @Nullable
    private ResourceLocation getPanelSource() {
        ItemStack panels = getItem(PANELS);
        return acceptsPanel(panels) ? BuiltInRegistries.BLOCK.getKey(((BlockItem) panels.getItem()).getBlock()) : null;
    }

    @Override
    public ModelData getModelData() {
        ModelData material = super.getModelData();
        ResourceLocation panel = getPanelSource();
        return panel == null ? material : material.derive().with(PANEL_SOURCE, panel).build();
    }

    @Override
    public void load(CompoundTag tag) {
        ResourceLocation previous = getPanelSource();
        super.load(tag);
        if (!Objects.equals(previous, getPanelSource())) {
            refreshModel();
        }
    }

    @Override
    public void setChanged() {
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(FurniturePanelerBlock.STACKS)) {
                int count = getItem(PANELS).getCount();
                int stacks = count > 54 ? 8 : (count + 7) / 8;
                if (state.getValue(FurniturePanelerBlock.STACKS) != stacks) {
                    level.setBlock(worldPosition, state.setValue(FurniturePanelerBlock.STACKS, stacks), Block.UPDATE_CLIENTS);
                }
            }
        }
        super.setChanged();
    }
}
