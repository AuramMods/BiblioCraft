package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import art.arcane.bibliocraft.book.ClipboardContent;
import art.arcane.bibliocraft.book.ClipboardContent.Page;
import art.arcane.bibliocraft.book.ClipboardContent.View;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ClipboardMenu extends AbstractContainerMenu {
    private final Player viewer;
    private final @Nullable InteractionHand hand;
    private final @Nullable DeskBlockEntity desk;
    private final ItemStack stack;
    private final Map<Integer, Page> displayed = new HashMap<>();
    private final View initial;

    public ClipboardMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, null, null, ItemStack.EMPTY, View.read(buffer));
    }

    public ClipboardMenu(int id, Player player, InteractionHand hand) {
        this(id, player, hand, null, player.getItemInHand(hand), View.of(player.getItemInHand(hand)));
    }

    public ClipboardMenu(int id, Player player, DeskBlockEntity desk) {
        this(id, player, null, desk, desk.getItem(DeskBlockEntity.OPEN_BOOK),
                View.of(desk.getItem(DeskBlockEntity.OPEN_BOOK)));
    }

    private ClipboardMenu(int id, Player viewer, @Nullable InteractionHand hand, @Nullable DeskBlockEntity desk,
                          ItemStack stack, View initial) {
        super(ModMenus.CLIPBOARD.get(), id);
        this.viewer = viewer;
        this.hand = hand;
        this.desk = desk;
        this.stack = stack;
        this.initial = initial;
        displayed.put(initial.current(), initial.page());
    }

    public static void openHeld(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, viewer) ->
                new ClipboardMenu(id, viewer, hand), stack.getHoverName()), View.of(stack)::write);
    }

    public static void openDesk(ServerPlayer player, DeskBlockEntity desk) {
        ItemStack stack = desk.getItem(DeskBlockEntity.OPEN_BOOK);
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, viewer) ->
                new ClipboardMenu(id, viewer, desk), stack.getHoverName()), View.of(stack)::write);
    }

    public View initial() {
        return initial;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player != viewer || !player.isAlive() || !BiblioConfig.COMMON.enableClipboard.get()) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        if (!stack.is(ModItems.BIBLIO_CLIPBOARD.get())) {
            return false;
        }
        return desk == null ? hand != null && player.getItemInHand(hand) == stack
                : BiblioConfig.COMMON.enableWritingdesk.get() && desk.stillValid(player)
                    && desk.getItem(DeskBlockEntity.OPEN_BOOK) == stack;
    }

    public @Nullable View visit(Player player, int pageIndex) {
        int count = ClipboardContent.count(stack);
        if (!stillValid(player) || pageIndex < 0 || pageIndex > count || pageIndex >= ClipboardContent.MAX_PAGES) {
            return null;
        }
        if (pageIndex == count) {
            ClipboardContent.save(stack, pageIndex, ClipboardContent.EMPTY_PAGE);
        }
        ClipboardContent.select(stack, pageIndex);
        changed();
        View view = View.of(stack);
        displayed.put(view.current(), view.page());
        return view;
    }

    public void edit(Player player, int pageIndex, Page desired) {
        Page before = displayed.get(pageIndex);
        if (!stillValid(player) || before == null || desired.equals(before)) {
            return;
        }
        Page current = ClipboardContent.page(stack, pageIndex);
        ClipboardContent.save(stack, pageIndex, current.merge(before, desired));
        displayed.put(pageIndex, desired);
        changed();
    }

    private void changed() {
        if (desk != null) {
            desk.setChanged();
        } else {
            viewer.getInventory().setChanged();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
