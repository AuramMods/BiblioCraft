package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.PrintingPressBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PrintingPressMenu extends FurnitureInventoryMenu {
    private static final int SLOT_COUNT = 4;
    private final DataSlot ticksLeft;

    public PrintingPressMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SLOT_COUNT));
    }

    public PrintingPressMenu(int id, Inventory inventory, Container press) {
        super(ModMenus.PRINTING_PRESS.get(), id, press, SLOT_COUNT);
        addPressSlot(press, PrintingPressBlockEntity.INK, 80, 24);
        addPressSlot(press, PrintingPressBlockEntity.PLATE, 80, 44);
        addPressSlot(press, PrintingPressBlockEntity.BLANK, 35, 52);
        addPressSlot(press, PrintingPressBlockEntity.RESULT, 125, 52);
        addPlayerSlots(inventory, 8, 94);
        ticksLeft = addDataSlot(press instanceof PrintingPressBlockEntity entity ? new DataSlot() {
            @Override
            public int get() {
                return entity.ticksLeft();
            }

            @Override
            public void set(int value) {}
        } : DataSlot.standalone());
    }

    private void addPressSlot(Container press, int index, int x, int y) {
        addSlot(new Slot(press, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PrintingPressBlockEntity.accepts(index, stack);
            }

            @Override
            public int getMaxStackSize() {
                return index == PrintingPressBlockEntity.PLATE ? 1 : super.getMaxStackSize();
            }
        });
    }

    public int ticksLeft() {
        return ticksLeft.get();
    }

    @Override
    public boolean stillValid(Player player) {
        return BiblioConfig.COMMON.enablePrintpressTypeMachine.get() && super.stillValid(player);
    }
}
