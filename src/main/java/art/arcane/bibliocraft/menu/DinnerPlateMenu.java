package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.DinnerPlateBlockEntity;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DinnerPlateMenu extends FurnitureInventoryMenu {
    public DinnerPlateMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(DinnerPlateBlockEntity.SLOT_COUNT));
    }

    public DinnerPlateMenu(int id, Inventory inventory, Container plate) {
        super(ModMenus.DINNER_PLATE.get(), id, plate, DinnerPlateBlockEntity.SLOT_COUNT);
        addFoodSlot(plate, 0, 80, 45);
        addFoodSlot(plate, 1, 35, 26);
        addFoodSlot(plate, 2, 125, 26);
        addPlayerSlots(inventory);
    }

    private void addFoodSlot(Container plate, int index, int x, int y) {
        addSlot(new Slot(plate, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DinnerPlateBlockEntity.isFood(stack);
            }
        });
    }

    @Override
    protected boolean canMoveIntoFurniture(ItemStack stack) {
        return DinnerPlateBlockEntity.isFood(stack);
    }
}
