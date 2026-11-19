package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.item.PortableInventory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

abstract class SingleSlotHeldItemMenu extends HeldItemMenu {
    private final InteractionHand hand;
    private final Item containerItem;
    private final BooleanSupplier enabled;
    private final int lockedSlot;

    SingleSlotHeldItemMenu(MenuType<?> type, int id, Inventory inventory, InteractionHand hand,
                           Item containerItem, BooleanSupplier enabled) {
        super(type, id, inventory.player, hand);
        this.hand = hand;
        this.containerItem = containerItem;
        this.enabled = enabled;
        lockedSlot = hand == InteractionHand.MAIN_HAND ? inventory.selected : Inventory.SLOT_OFFHAND;
        var storage = new SimpleContainer(1);
        storage.setItem(0, PortableInventory.read(heldStack));
        addSlot(new Slot(storage, 0, 78, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.is(containerItem);
            }
        });
        for (int slot = 0; slot < 27; slot++) {
            addSlot(new Slot(inventory, slot + 9, 6 + slot % 9 * 18, 133 + slot / 9 * 18));
        }
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot, 6 + slot * 18, 191) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return getContainerSlot() != lockedSlot;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return getContainerSlot() != lockedSlot;
                }
            });
        }
        if (!owner.level().isClientSide) {
            storage.addListener(container -> {
                if (stillValid(owner)) {
                    PortableInventory.write(heldStack, container.getItem(0));
                    inventory.setChanged();
                }
            });
        }
    }

    public ItemStack storedItem() {
        return getSlot(0).getItem();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide
                ? player == owner && player.getItemInHand(hand).is(containerItem)
                : super.stillValid(player) && heldStack.is(containerItem) && enabled.getAsBoolean();
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (stillValid(player) && slot != -999 && (type != ClickType.SWAP || button != lockedSlot)) {
            super.clicked(slot, button, type, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = getSlot(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        boolean moved = index == 0 ? moveItemStackTo(current, 1, slots.size(), true)
                : moveItemStackTo(current, 0, 1, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, current);
        return original;
    }
}
