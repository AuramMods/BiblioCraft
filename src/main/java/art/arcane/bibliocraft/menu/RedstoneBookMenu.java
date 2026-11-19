package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public final class RedstoneBookMenu extends HeldItemMenu {
    public static final int TITLE_LENGTH = 42;
    private final InteractionHand hand;
    private final String initialTitle;

    public RedstoneBookMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, buffer.readEnum(InteractionHand.class), buffer.readUtf(TITLE_LENGTH));
    }

    public RedstoneBookMenu(int id, Player player, InteractionHand hand) {
        this(id, player, hand, title(player.getItemInHand(hand)));
    }

    private RedstoneBookMenu(int id, Player player, InteractionHand hand, String initialTitle) {
        super(ModMenus.REDSTONE_BOOK.get(), id, player, hand);
        this.hand = hand;
        this.initialTitle = initialTitle;
    }

    public static void open(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, viewer) ->
                new RedstoneBookMenu(id, viewer, hand), stack.getHoverName()), buffer -> {
            buffer.writeEnum(hand);
            buffer.writeUtf(title(stack), TITLE_LENGTH);
        });
    }

    private static String title(ItemStack stack) {
        return stack.hasCustomHoverName() ? clean(stack.getHoverName().getString()) : "";
    }

    private static String clean(String name) {
        return CatalogContents.bounded(SharedConstants.filterText(name), TITLE_LENGTH).strip();
    }

    public String initialTitle() {
        return initialTitle;
    }

    public void rename(Player player, String name) {
        if (stillValid(player)) {
            String title = clean(name);
            if (title.isEmpty()) {
                heldStack.resetHoverName();
            } else {
                heldStack.setHoverName(Component.literal(title));
            }
            player.getInventory().setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) {
            return player == owner && player.getItemInHand(hand).is(ModItems.BIBLIO_RED_BOOK.get());
        }
        return super.stillValid(player) && heldStack.is(ModItems.BIBLIO_RED_BOOK.get()) && BiblioConfig.COMMON.enableRedstonebook.get();
    }
}
