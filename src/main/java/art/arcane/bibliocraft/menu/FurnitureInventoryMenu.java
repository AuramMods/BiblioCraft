package art.arcane.bibliocraft.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

abstract class FurnitureInventoryMenu extends AbstractContainerMenu {
    private final Container furniture;
    private final int furnitureSlots;

    protected FurnitureInventoryMenu(MenuType<?> type, int id, Container furniture, int furnitureSlots) {
        super(type, id);
        checkContainerSize(furniture, furnitureSlots);
        this.furniture = furniture;
        this.furnitureSlots = furnitureSlots;
    }

    protected final void addPlayerSlots(Inventory inventory) {
        addPlayerSlots(inventory, 8, 84);
    }

    protected final void addPlayerSlots(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (index >= furnitureSlots && !canMoveIntoFurniture(stack)) {
            return ItemStack.EMPTY;
        }
        ItemStack original = stack.copy();
        boolean moved = index < furnitureSlots
                ? moveItemStackTo(stack, furnitureSlots, slots.size(), true)
                : moveItemStackTo(stack, 0, furnitureSlots, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    protected boolean canMoveIntoFurniture(ItemStack stack) {
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return furniture.stillValid(player);
    }
}
