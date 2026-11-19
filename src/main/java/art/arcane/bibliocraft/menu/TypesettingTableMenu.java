package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.TypesettingTableBlockEntity;
import art.arcane.bibliocraft.book.PrintingLibrary;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.network.ModNetwork;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TypesettingTableMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 8;
    private static final Page EMPTY = new Page(0, 1, List.of(), null);
    private final @Nullable TypesettingTableBlockEntity table;
    private final @Nullable ServerPlayer owner;
    private Page page = EMPTY;
    private int pageIndex;
    private long seenRevision = -1;
    private @Nullable UUID seenSelection;

    public TypesettingTableMenu(int id, Inventory inventory) {
        this(id, null, null);
    }

    public TypesettingTableMenu(int id, @Nullable TypesettingTableBlockEntity table, @Nullable ServerPlayer owner) {
        super(ModMenus.TYPESETTING_TABLE.get(), id);
        this.table = table;
        this.owner = owner;
    }

    public static void open(ServerPlayer player, TypesettingTableBlockEntity table) {
        player.openMenu(new SimpleMenuProvider((id, inventory, visitor) -> new TypesettingTableMenu(id, table, player),
                Component.translatable("gui.bibliocraft.typesetting.library")));
    }

    @Override
    public boolean stillValid(Player player) {
        return table == null || (BiblioConfig.COMMON.enablePrintpressTypeMachine.get() && table.stillValid(player));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (table == null || owner == null || owner.containerMenu != this || !stillValid(owner)) {
            return;
        }
        var library = PrintingLibrary.get(owner.serverLevel());
        if (seenRevision != library.revision() || !Objects.equals(seenSelection, table.selected())) {
            sendPage(library);
        }
    }

    public void apply(ServerPlayer player, Action action, @Nullable UUID bookId) {
        if (table == null || owner != player || player.containerMenu != this || !stillValid(player)) {
            return;
        }
        var library = PrintingLibrary.get(player.serverLevel());
        switch (action) {
            case PREVIOUS -> pageIndex = Math.max(0, pageIndex - 1);
            case NEXT -> pageIndex = Math.min(page.total() - 1, pageIndex + 1);
            case SELECT -> {
                var entry = library.find(bookId);
                if (entry != null && entry.canRead(player)) {
                    table.select(entry.id());
                    player.displayClientMessage(Component.translatable("message.bibliocraft.typesetting.selected",
                            entry.title()), true);
                    player.closeContainer();
                    return;
                }
            }
            case SHARE -> {
                if (bookId != null) {
                    library.toggleVisibility(bookId, player);
                }
            }
            case DELETE -> {
                if (bookId != null) {
                    library.delete(bookId, player);
                }
            }
        }
        sendPage(library);
    }

    private void sendPage(PrintingLibrary library) {
        if (table == null || owner == null) {
            return;
        }
        var visible = library.visibleTo(owner);
        int total = Math.max(1, (visible.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        pageIndex = Mth.clamp(pageIndex, 0, total - 1);
        int start = pageIndex * PAGE_SIZE;
        List<Row> rows = visible.subList(start, Math.min(start + PAGE_SIZE, visible.size())).stream()
                .map(entry -> new Row(entry.id(), entry.title(), entry.author(), entry.shared(), entry.canManage(owner)))
                .toList();
        var selected = library.find(table.selected());
        UUID selection = selected != null && selected.canRead(owner) ? selected.id() : null;
        page = new Page(pageIndex, total, rows, selection);
        seenRevision = library.revision();
        seenSelection = table.selected();
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> owner),
                new ModNetwork.TypesettingPagePacket(containerId, page));
    }

    public Page page() {
        return page;
    }

    public void receive(Page update) {
        if (table == null) {
            page = update;
        }
    }

    public enum Action {
        PREVIOUS, NEXT, SELECT, SHARE, DELETE
    }

    public record Row(UUID id, String title, String author, boolean shared, boolean canManage) {
        private void write(FriendlyByteBuf buffer) {
            buffer.writeUUID(id).writeUtf(title, PrintingLibrary.TITLE_LENGTH)
                    .writeUtf(author, PrintingLibrary.AUTHOR_LENGTH).writeBoolean(shared).writeBoolean(canManage);
        }

        private static Row read(FriendlyByteBuf buffer) {
            return new Row(buffer.readUUID(), buffer.readUtf(PrintingLibrary.TITLE_LENGTH),
                    buffer.readUtf(PrintingLibrary.AUTHOR_LENGTH), buffer.readBoolean(), buffer.readBoolean());
        }
    }

    public record Page(int index, int total, List<Row> rows, @Nullable UUID selected) {
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(index).writeVarInt(total).writeNullable(selected, FriendlyByteBuf::writeUUID);
            buffer.writeCollection(rows, (out, row) -> row.write(out));
        }

        public static Page read(FriendlyByteBuf buffer) {
            int index = buffer.readVarInt();
            int total = buffer.readVarInt();
            UUID selected = buffer.readNullable(FriendlyByteBuf::readUUID);
            return new Page(index, total,
                    buffer.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, PAGE_SIZE), Row::read), selected);
        }
    }
}
