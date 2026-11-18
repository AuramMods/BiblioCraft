package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.FurniturePanelerBlockEntity;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FurniturePanelerMenu extends AbstractContainerMenu {
    public static final int RESULT = FurniturePanelerBlockEntity.SLOT_COUNT;

    private final Container inputs;
    private final Container output = new SimpleContainer(1);

    public FurniturePanelerMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(FurniturePanelerBlockEntity.SLOT_COUNT));
    }

    public FurniturePanelerMenu(int id, Inventory inventory, Container inputs) {
        super(ModMenus.FURNITURE_PANELER.get(), id);
        checkContainerSize(inputs, FurniturePanelerBlockEntity.SLOT_COUNT);
        this.inputs = inputs;
        addSlot(new Slot(inputs, FurniturePanelerBlockEntity.PANELS, 80, 27) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FurniturePanelerBlockEntity.acceptsPanel(stack);
            }
        });
        addSlot(new Slot(inputs, FurniturePanelerBlockEntity.FURNITURE, 62, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FurniturePanelerBlockEntity.acceptsFurniture(stack);
            }
        });
        addSlot(new Slot(output, 0, 98, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (inputs instanceof FurniturePanelerBlockEntity paneler) {
                    paneler.consumeInputs();
                    refreshOutput();
                }
                super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        refreshOutput();
    }

    private void refreshOutput() {
        if (inputs instanceof FurniturePanelerBlockEntity paneler) {
            ItemStack result = paneler.getResult();
            if (!ItemStack.matches(output.getItem(0), result)) {
                output.setItem(0, result);
            }
        }
    }

    @Override
    public void broadcastChanges() {
        refreshOutput();
        super.broadcastChanges();
    }

    @Override
    public void clicked(int index, int button, ClickType type, Player player) {
        refreshOutput();
        super.clicked(index, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        refreshOutput();
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index <= RESULT) {
            moved = moveItemStackTo(stack, RESULT + 1, slots.size(), true);
        } else if (FurniturePanelerBlockEntity.acceptsPanel(stack)) {
            moved = moveItemStackTo(stack, 0, 1, false);
        } else if (FurniturePanelerBlockEntity.acceptsFurniture(stack)) {
            moved = moveItemStackTo(stack, 1, 2, false);
        } else {
            return ItemStack.EMPTY;
        }
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

    @Override
    public boolean stillValid(Player player) {
        return inputs.stillValid(player);
    }
}
