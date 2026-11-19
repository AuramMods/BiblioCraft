package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.book.SlottedBookContents;
import art.arcane.bibliocraft.book.SlottedBookContents.Label;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public final class SlottedBookMenu extends SingleSlotHeldItemMenu {
    private final Label label;

    public SlottedBookMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readEnum(InteractionHand.class), Label.read(buffer));
    }

    public SlottedBookMenu(int id, Inventory inventory, InteractionHand hand) {
        this(id, inventory, hand, SlottedBookContents.label(inventory.player.getItemInHand(hand)));
    }

    private SlottedBookMenu(int id, Inventory inventory, InteractionHand hand, Label label) {
        super(ModMenus.SLOTTED_BOOK.get(), id, inventory, hand, ModItems.SLOTTED_BOOK.get(),
                BiblioConfig.COMMON.enableSlottedBook::get);
        this.label = label;
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
}
