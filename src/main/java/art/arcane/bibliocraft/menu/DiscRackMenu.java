package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.DiscRackBlockEntity;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DiscRackMenu extends FurnitureInventoryMenu {
    public DiscRackMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(DiscRackBlockEntity.SLOT_COUNT));
    }

    public DiscRackMenu(int id, Inventory inventory, Container rack) {
        super(ModMenus.DISC_RACK.get(), id, rack, DiscRackBlockEntity.SLOT_COUNT);
        for (int slot = 0; slot < DiscRackBlockEntity.SLOT_COUNT; slot++) {
            addSlot(new Slot(rack, slot, 8 + slot * 18, 34) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return DiscRackBlockEntity.isDisc(stack);
                }
            });
        }
        addPlayerSlots(inventory);
    }

    @Override
    protected boolean canMoveIntoFurniture(ItemStack stack) {
        return DiscRackBlockEntity.isDisc(stack);
    }
}
