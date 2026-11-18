package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.block.RackType;
import art.arcane.bibliocraft.blockentity.RackBlockEntity;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RackMenu extends FurnitureInventoryMenu {
    private final RackType type;

    public RackMenu(int id, Inventory inventory, RackType type) {
        this(id, inventory, new SimpleContainer(type.size()), type);
    }

    public RackMenu(int id, Inventory inventory, RackBlockEntity rack) {
        this(id, inventory, rack, rack.type());
    }

    private RackMenu(int id, Inventory inventory, Container rack, RackType type) {
        super(type == RackType.POTIONS ? ModMenus.POTION_SHELF.get() : ModMenus.TOOL_RACK.get(), id, rack, type.size());
        this.type = type;

        for (int slot = 0; slot < type.size(); slot++) {
            addSlot(new Slot(rack, slot, type.menuX(slot), type.menuY(slot)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return type.accepts(stack);
                }
            });
        }
        addPlayerSlots(inventory);
    }

    public RackType type() {
        return type;
    }

    @Override
    protected boolean canMoveIntoFurniture(ItemStack stack) {
        return type.accepts(stack);
    }
}
