package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class DeskBookMenu extends AbstractContainerMenu {
    public static final int MAX_PAGES = 100;
    public static final int MAX_PAGE_LENGTH = 1024;
    public static final int MAX_TITLE_LENGTH = 32;
    @Nullable
    private final DeskBlockEntity desk;
    private final ItemStack original;
    private final ItemStack snapshot;
    private final int page;

    public DeskBookMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, null, data.readItem(), data.readVarInt());
    }

    public DeskBookMenu(int id, DeskBlockEntity desk) {
        this(id, desk, desk.getItem(DeskBlockEntity.OPEN_BOOK), desk.getPage());
    }

    private DeskBookMenu(int id, @Nullable DeskBlockEntity desk, ItemStack book, int page) {
        super(ModMenus.DESK_BOOK.get(), id);
        this.desk = desk;
        this.original = book;
        this.snapshot = book.copy();
        this.page = page;
    }

    public ItemStack getBook() {
        return snapshot;
    }

    public int initialPage() {
        return page;
    }

    @Override
    public boolean stillValid(Player player) {
        return desk == null || (BiblioConfig.COMMON.enableWritingdesk.get() && desk.stillValid(player)
                && desk.getItem(DeskBlockEntity.OPEN_BOOK) == original && ItemStack.matches(snapshot, original));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public void turnPage(Player player, int selected) {
        if (desk != null && player.containerMenu == this && stillValid(player) && selected >= 0) {
            int lastPage = original.hasTag() ? Math.max(0, original.getTag().getList("pages", Tag.TAG_STRING).size() - 1) : 0;
            desk.setPage(Math.min(selected, lastPage));
        }
    }

    public boolean save(Player player, List<String> pages, Optional<String> title, int selected) {
        if (desk == null || player.containerMenu != this || !stillValid(player) || !original.is(Items.WRITABLE_BOOK)
                || pages.isEmpty() || pages.size() > MAX_PAGES || pages.stream().anyMatch(page -> page.length() > MAX_PAGE_LENGTH)) {
            return false;
        }
        String signedTitle = title.map(SharedConstants::filterText).map(String::strip).orElse("");
        if (title.isPresent() && (signedTitle.isEmpty() || signedTitle.length() > MAX_TITLE_LENGTH)) {
            return false;
        }
        ItemStack result = title.isPresent() ? new ItemStack(Items.WRITTEN_BOOK) : original.copy();
        if (title.isPresent() && original.hasTag()) {
            result.setTag(original.getTag().copy());
        }
        ListTag contents = new ListTag();
        for (String page : pages) {
            String text = SharedConstants.filterText(page, true);
            contents.add(StringTag.valueOf(title.isPresent() ? Component.Serializer.toJson(Component.literal(text)) : text));
        }
        var tag = result.getOrCreateTag();
        tag.put("pages", contents);
        tag.remove("filtered_pages");
        if (title.isPresent()) {
            tag.putString("author", player.getGameProfile().getName());
            tag.putString("title", signedTitle);
            tag.putBoolean("resolved", true);
        }
        desk.setItem(DeskBlockEntity.OPEN_BOOK, result);
        desk.setPage(Mth.clamp(selected, 0, pages.size() - 1));
        return true;
    }
}
