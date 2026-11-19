package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import art.arcane.bibliocraft.book.BigBookContent;
import art.arcane.bibliocraft.book.BigBookContent.Chapter;
import art.arcane.bibliocraft.book.BigBookContent.Header;
import art.arcane.bibliocraft.book.BigBookContent.Page;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BigBookMenu extends AbstractContainerMenu {
    private final Player viewer;
    private final @Nullable DeskBlockEntity desk;
    private final @Nullable InteractionHand hand;
    private final ItemStack book;
    private final Opened opened;
    private final Map<Integer, Page> readPages = new HashMap<>();
    private final Map<Integer, Page> edits = new HashMap<>();
    private int pageCount;
    private boolean finished;

    public BigBookMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, null, null, ItemStack.EMPTY, Opened.read(buffer));
    }

    public BigBookMenu(int id, Player viewer, DeskBlockEntity desk) {
        this(id, viewer, desk, null, desk.getItem(DeskBlockEntity.OPEN_BOOK),
                describe(desk.getItem(DeskBlockEntity.OPEN_BOOK), desk));
    }

    public BigBookMenu(int id, Player viewer, InteractionHand hand) {
        this(id, viewer, null, hand, viewer.getItemInHand(hand), describe(viewer.getItemInHand(hand), null));
    }

    private BigBookMenu(int id, Player viewer, @Nullable DeskBlockEntity desk, @Nullable InteractionHand hand,
                        ItemStack book, Opened opened) {
        super(ModMenus.BIG_BOOK.get(), id);
        this.viewer = viewer;
        this.desk = desk;
        this.hand = hand;
        this.book = book;
        this.opened = opened;
        pageCount = opened.header().pages();
        readPages.put(opened.header().bookmark(), opened.page());
    }

    public static void openHeld(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Opened opened = describe(stack, null);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, viewer) -> new BigBookMenu(id, viewer, null, hand, stack, opened), title(stack, opened.header())),
                opened::write);
    }

    public static void openDesk(ServerPlayer player, DeskBlockEntity desk) {
        ItemStack stack = desk.getItem(DeskBlockEntity.OPEN_BOOK);
        Opened opened = describe(stack, desk);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, viewer) -> new BigBookMenu(id, viewer, desk, null, stack, opened), title(stack, opened.header())),
                opened::write);
    }

    private static Component title(ItemStack stack, Header header) {
        return header.title().isEmpty() ? stack.getHoverName() : Component.literal(header.title());
    }

    private static Opened describe(ItemStack book, @Nullable DeskBlockEntity desk) {
        Header header = BigBookContent.header(book, BiblioConfig.COMMON.defaultBigBookTextScale.get());
        if (desk != null) {
            header = header.withBookmark(desk.getPage());
        }
        return new Opened(header, BigBookContent.page(book, header.bookmark(), header.defaultSize()));
    }

    public Header initialHeader() {
        return opened.header();
    }

    public @Nullable Page pageForScreen(int page) {
        return readPages.get(page);
    }

    public void acceptPage(int index, Page page) {
        readPages.put(index, page);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player != viewer || !player.isAlive() || !BiblioConfig.COMMON.enableBigBook.get()) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        if (!book.is(ModItems.BIG_BOOK.get()) || opened.header().signed() != BigBookContent.signed(book)) {
            return false;
        }
        if (desk != null) {
            return BiblioConfig.COMMON.enableWritingdesk.get() && desk.stillValid(player)
                    && desk.getItem(DeskBlockEntity.OPEN_BOOK) == book;
        }
        return hand != null && player.getItemInHand(hand) == book;
    }

    public Optional<Page> visit(Player player, int page) {
        if (!allowed(player) || page < 0 || page >= BigBookContent.MAX_PAGES) {
            return Optional.empty();
        }
        int savedCount = BigBookContent.header(book, opened.header().defaultSize()).pages();
        pageCount = Math.max(pageCount, savedCount);
        if (page >= pageCount) {
            if (opened.header().signed() || page != pageCount) {
                return Optional.empty();
            }
            pageCount++;
        }
        if (desk != null && page < savedCount && desk.getPage() != page) {
            desk.setPage(page);
        }
        Page content = readPages.computeIfAbsent(page,
                index -> BigBookContent.page(book, index, opened.header().defaultSize()));
        return Optional.of(content);
    }

    public boolean stage(Player player, int page, Page edited) {
        if (!allowed(player) || opened.header().signed() || !readPages.containsKey(page)) {
            return false;
        }
        if (readPages.get(page).equals(edited)) {
            edits.remove(page);
        } else {
            edits.put(page, edited);
        }
        return true;
    }

    public boolean finish(Player player, int page, List<Chapter> chapterEdits, Optional<String> signingTitle) {
        if (!allowed(player) || page < 0 || page >= pageCount || chapterEdits.size() != BigBookContent.CHAPTERS) {
            return false;
        }
        int defaultSize = opened.header().defaultSize();
        Header actual = BigBookContent.header(book, defaultSize);
        String title = signingTitle.map(BigBookContent::validTitle).orElse("");
        if (signingTitle.isPresent() && (title.isEmpty() || actual.signed())) {
            return false;
        }
        int mergedCount = Math.max(actual.pages(), pageCount);
        List<Chapter> chapters = actual.chapters();
        Map<Integer, Page> mergedPages = new HashMap<>();
        if (!actual.signed()) {
            chapters = new ArrayList<>(BigBookContent.CHAPTERS);
            for (int index = 0; index < BigBookContent.CHAPTERS; index++) {
                chapters.add(actual.chapters().get(index).merge(opened.header().chapters().get(index),
                        chapterEdits.get(index)).inBook(mergedCount));
            }
            edits.forEach((index, edit) -> {
                Page current = BigBookContent.page(book, index, defaultSize);
                mergedPages.put(index, current.merge(readPages.get(index), edit));
            });
        }
        boolean signed = actual.signed() || signingTitle.isPresent();
        var update = new Header(mergedCount, page, signed, signingTitle.isPresent() ? title : actual.title(),
                signingTitle.isPresent() ? player.getGameProfile().getName() : actual.author(), defaultSize, chapters);
        BigBookContent.update(book, update, mergedPages, !chapters.equals(actual.chapters()));
        if (desk != null) {
            desk.setPage(Mth.clamp(page, 0, mergedCount - 1));
        } else {
            player.getInventory().setChanged();
        }
        finished = true;
        return true;
    }

    private boolean allowed(Player player) {
        return !finished && player.containerMenu == this && stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private record Opened(Header header, Page page) {
        void write(FriendlyByteBuf buffer) {
            header.write(buffer);
            page.write(buffer);
        }

        static Opened read(FriendlyByteBuf buffer) {
            return new Opened(Header.read(buffer), Page.read(buffer));
        }
    }
}
