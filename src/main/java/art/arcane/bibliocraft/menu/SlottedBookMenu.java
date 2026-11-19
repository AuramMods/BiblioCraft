package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.book.SlottedBookContents;
import art.arcane.bibliocraft.book.SlottedBookContents.Label;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public final class SlottedBookMenu extends HeldItemMenu {
    private final InteractionHand hand;
    private final int lockedSlot;
    private final Label label;

    public SlottedBookMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readEnum(InteractionHand.class), Label.read(buffer));
    }

    public SlottedBookMenu(int id, Inventory inventory, InteractionHand hand) {
        this(id, inventory, hand, SlottedBookContents.label(inventory.player.getItemInHand(hand)));
    }

    private SlottedBookMenu(int id, Inventory inventory, InteractionHand hand, Label label) {
        super(ModMenus.SLOTTED_BOOK.get(), id, inventory.player, hand);
        this.hand = hand;
        this.label = label;
        lockedSlot = hand == InteractionHand.MAIN_HAND ? inventory.selected : Inventory.SLOT_OFFHAND;
        var storage = new SimpleContainer(1);
        storage.setItem(0, SlottedBookContents.stored(heldStack));
        addSlot(new Slot(storage, 0, 78, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.is(ModItems.SLOTTED_BOOK.get());
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
                    SlottedBookContents.store(heldStack, container.getItem(0));
                    inventory.setChanged();
                }
            });
        }
    }

    public static void open(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, owner) -> new SlottedBookMenu(id, inventory, hand), stack.getHoverName()), buffer -> {
            buffer.writeEnum(hand);
            SlottedBookContents.label(stack).write(buffer);
        });
    }

    public Label label() {
        return label;
    }

    public void saveLabel(Player player, Label edited) {
        if (stillValid(player)) {
            edited.save(heldStack);
            player.getInventory().setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) {
            return player == owner && player.getItemInHand(hand).is(ModItems.SLOTTED_BOOK.get());
        }
        return super.stillValid(player) && heldStack.is(ModItems.SLOTTED_BOOK.get()) && BiblioConfig.COMMON.enableSlottedBook.get();
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
