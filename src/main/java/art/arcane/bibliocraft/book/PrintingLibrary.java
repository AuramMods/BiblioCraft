package art.arcane.bibliocraft.book;

import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.crafting.RecordedRecipe;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PrintingLibrary extends SavedData {
    public static final int TITLE_LENGTH = 96;
    public static final int AUTHOR_LENGTH = 32;
    private final Map<UUID, Entry> books = new LinkedHashMap<>();
    private long revision;

    public static PrintingLibrary get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                PrintingLibrary::load, PrintingLibrary::new, "bibliocraft_printing_library");
    }

    public static boolean isPublishable(ItemStack book) {
        if (book.is(Items.WRITTEN_BOOK)) {
            return book.hasTag() && WrittenBookItem.makeSureTagIsValid(book.getTag());
        }
        return book.is(ModItems.BIG_BOOK.get()) && BigBookContent.signed(book)
                || book.is(ModItems.RECIPE_BOOK.get()) && RecordedRecipe.isSigned(book)
                || book.is(ModItems.STOCKROOM_CATALOG.get());
    }

    public List<Entry> visibleTo(Player player) {
        return books.values().stream().filter(book -> book.canRead(player))
                .sorted(Comparator.comparing(Entry::title, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Entry::author, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public @Nullable Entry find(@Nullable UUID id) {
        return books.get(id);
    }

    public @Nullable Entry publish(ItemStack book, Player player) {
        if (!isPublishable(book)) {
            return null;
        }
        var tag = book.getTag();
        String signedAuthor = tag == null ? "" : tag.getString("author");
        String playerName = player.getGameProfile().getName();
        String author = CatalogContents.bounded(signedAuthor.isBlank() ? playerName : signedAuthor, AUTHOR_LENGTH);
        String title = CatalogContents.bounded(book.getHoverName().getString(), TITLE_LENGTH);
        UUID owner = author.equalsIgnoreCase(playerName) ? player.getUUID() : null;
        ItemStack snapshot = book.copyWithCount(1);
        for (Entry entry : books.values()) {
            if (entry.author().equalsIgnoreCase(author) && ItemStack.isSameItemSameTags(entry.book(), snapshot)) {
                return entry;
            }
        }
        var entry = new Entry(UUID.randomUUID(), owner, title, author,
                BiblioConfig.COMMON.enablePublicTypesettingBooks.get(), snapshot);
        books.put(entry.id(), entry);
        changed();
        return entry;
    }

    public boolean toggleVisibility(UUID id, Player player) {
        Entry entry = find(id);
        if (entry == null || !entry.canManage(player)) {
            return false;
        }
        books.put(id, new Entry(id, entry.owner(), entry.title(), entry.author(), !entry.shared(), entry.book()));
        changed();
        return true;
    }

    public boolean delete(UUID id, Player player) {
        Entry entry = find(id);
        if (entry == null || !entry.canManage(player)) {
            return false;
        }
        books.remove(id);
        changed();
        return true;
    }

    public long revision() {
        return revision;
    }

    private void changed() {
        revision++;
        setDirty();
    }

    public static PrintingLibrary load(CompoundTag tag) {
        var library = new PrintingLibrary();
        ListTag entries = tag.getList("Books", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag stored = entries.getCompound(i);
            ItemStack book = ItemStack.of(stored.getCompound("Book"));
            if (stored.hasUUID("Id") && isPublishable(book)) {
                UUID id = stored.getUUID("Id");
                UUID owner = stored.hasUUID("Owner") ? stored.getUUID("Owner") : null;
                library.books.put(id, new Entry(id, owner,
                        CatalogContents.bounded(stored.getString("Title"), TITLE_LENGTH),
                        CatalogContents.bounded(stored.getString("Author"), AUTHOR_LENGTH),
                        stored.getBoolean("Public"), book));
            }
        }
        return library;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        var entries = new ListTag();
        for (Entry book : books.values()) {
            var stored = new CompoundTag();
            stored.putUUID("Id", book.id());
            if (book.owner() != null) {
                stored.putUUID("Owner", book.owner());
            }
            stored.putString("Title", book.title());
            stored.putString("Author", book.author());
            stored.putBoolean("Public", book.shared());
            stored.put("Book", book.book().save(new CompoundTag()));
            entries.add(stored);
        }
        tag.put("Books", entries);
        return tag;
    }

    public record Entry(UUID id, @Nullable UUID owner, String title, String author, boolean shared, ItemStack book) {
        public boolean canManage(Player player) {
            return player.isCreative() || (owner == null
                    ? author.equalsIgnoreCase(player.getGameProfile().getName()) : owner.equals(player.getUUID()));
        }

        public boolean canRead(Player player) {
            return shared || canManage(player);
        }
    }
}
