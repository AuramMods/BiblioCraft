package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import art.arcane.bibliocraft.book.BigBookContent;
import art.arcane.bibliocraft.book.BigBookContent.Chapter;
import art.arcane.bibliocraft.book.BigBookContent.Header;
import art.arcane.bibliocraft.book.BigBookContent.Line;
import art.arcane.bibliocraft.book.BigBookContent.Page;
import art.arcane.bibliocraft.menu.BigBookMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class BigBookGameTests {
    private static final BlockPos DESK = new BlockPos(2, 1, 2);

    private BigBookGameTests() {}

    @GameTest(template = "empty")
    public static void readsLegacyChaptersScalesFormattingAndSignedName(GameTestHelper helper) {
        ItemStack book = new ItemStack(ModItems.BIG_BOOK.get());
        var tag = book.getOrCreateTag();
        tag.putInt("pagesTotal", 3);
        tag.putInt("pagesCurrent", 1);
        tag.putBoolean("signed", true);
        tag.putString("author", "Legacy Writer");
        book.getOrCreateTagElement("display").putString("Name", "§fField Notes");
        var pages = new CompoundTag();
        var strings = new ListTag();
        strings.add(StringTag.valueOf("§2Notes §labout the forest 🌲"));
        pages.put("page1", strings);
        pages.putIntArray("pageScale1", new int[]{5});
        tag.put("pages", pages);
        var tabs = new CompoundTag();
        tabs.putIntArray("chapBools", new int[]{1});
        tabs.putIntArray("chapPages", new int[]{1});
        tabs.putString("chapline0", "Field");
        tabs.putString("chapline1", "Notes");
        tag.put("chapters", tabs);

        ItemStack restored = ItemStack.of(book.save(new CompoundTag()));
        Header header = BigBookContent.header(restored, 2);
        helper.assertTrue(header.signed() && header.pages() == 3 && header.bookmark() == 1
                        && header.title().equals("Field Notes") && header.author().equals("Legacy Writer")
                        && header.chapters().get(0).equals(new Chapter(true, 1, "Field", "Notes")),
                "The original nested chapter and bookmark tags should still open correctly");
        Page page = BigBookContent.page(restored, 1, 2);
        helper.assertTrue(page.lines().get(0).equals(new Line("§2Notes §labout the forest 🌲", 5))
                        && page.lines().get(1).size() == 2 && restored.hasFoil(),
                "Reading should preserve styles and apply the configured default to missing sizes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deskEditsMergeAndSigningFreezesTheBook(GameTestHelper helper) {
        DeskBlockEntity desk = desk(helper);
        ItemStack book = new ItemStack(ModItems.BIG_BOOK.get());
        desk.setItem(DeskBlockEntity.OPEN_BOOK, book);
        Player first = player(helper);
        Player second = player(helper);
        var one = new BigBookMenu(1, first, desk);
        var two = new BigBookMenu(2, second, desk);
        first.containerMenu = one;
        second.containerMenu = two;
        var opened = one.initialHeader();
        helper.assertTrue(one.visit(first, 2).isEmpty() && one.visit(first, 1).isPresent(),
                "Only the next unsigned page should be appendable");
        helper.assertTrue(one.stage(first, 1, line(one.pageForScreen(1), 0, "§9Page two", 3))
                        && one.stage(first, 0, line(one.pageForScreen(0), 0, "One", 4))
                        && two.stage(second, 0, line(two.pageForScreen(0), 1, "Two", 5)),
                "Both viewers should be able to stage changes to separate lines");
        var tabs = new ArrayList<>(opened.chapters());
        tabs.set(0, new Chapter(true, 1, "Second page", "Notes"));
        helper.assertTrue(one.finish(first, 1, tabs, Optional.empty())
                        && two.finish(second, 0, two.initialHeader().chapters(), Optional.of("Our Notes")),
                "Both drafts should save and the second author should be able to sign afterward");
        var saved = BigBookContent.header(book, 0);
        helper.assertTrue(saved.signed() && saved.pages() == 2 && saved.title().equals("Our Notes")
                        && saved.author().equals(second.getGameProfile().getName())
                        && saved.chapters().get(0).equals(tabs.get(0))
                        && desk.getPage() == 0 && book.getHoverName().getString().equals("Our Notes"),
                "Signing should preserve the other viewer's chapter, authoritatively set the author and keep the bookmark");
        Page firstPage = BigBookContent.page(book, 0, 0);
        helper.assertTrue(firstPage.lines().get(0).equals(new Line("One", 4))
                        && firstPage.lines().get(1).equals(new Line("Two", 5))
                        && BigBookContent.page(book, 1, 0).lines().get(0).equals(new Line("§9Page two", 3)),
                "Separate edits should merge across lines and pages");
        var reader = new BigBookMenu(3, first, desk);
        first.containerMenu = reader;
        helper.assertTrue(reader.visit(first, 2).isEmpty() && reader.visit(first, 1).isPresent()
                        && !reader.stage(first, 1, line(reader.pageForScreen(1), 0, "Overwrite", 0))
                        && reader.finish(first, 1, reader.initialHeader().chapters(), Optional.empty())
                        && desk.getPage() == 1 && BigBookContent.bookmark(book) == 1,
                "A signed reader can turn pages and save the new bookmark");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void draftsFollowTheirActualHeldBookOrAccessibleDesk(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack first = new ItemStack(ModItems.BIG_BOOK.get());
        player.setItemInHand(InteractionHand.OFF_HAND, first);
        var hand = new BigBookMenu(1, player, InteractionHand.OFF_HAND);
        player.containerMenu = hand;
        helper.assertTrue(!hand.stage(player, 5, Page.blank(0))
                        && hand.stage(player, 0, line(hand.pageForScreen(0), 0, "Keep this", 1)),
                "The menu should only accept pages opened by this viewer");
        player.setItemInHand(InteractionHand.OFF_HAND, first.copy());
        helper.assertTrue(!hand.finish(player, 0, hand.initialHeader().chapters(), Optional.empty())
                        && !first.hasTag() && !player.getOffhandItem().hasTag(),
                "A staged edit should leave both items intact if the held book is replaced");

        var desk = desk(helper);
        desk.setItem(DeskBlockEntity.OPEN_BOOK, first);
        var deskMenu = new BigBookMenu(2, player, desk);
        player.containerMenu = deskMenu;
        helper.assertTrue(deskMenu.stage(player, 0, line(deskMenu.pageForScreen(0), 0, "Cannot save", 0)),
                "The guest can prepare a draft while the desk is accessible");
        desk.toggleLock(player(helper));
        helper.assertTrue(!deskMenu.finish(player, 0, deskMenu.initialHeader().chapters(), Optional.empty())
                        && !first.hasTag(), "A newly locked desk should reject the guest's pending edits");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void largeBooksUseBoundedPagesAndKeepDataOnSurvivalDrops(GameTestHelper helper) {
        List<Line> rows = new ArrayList<>(Collections.nCopies(BigBookContent.LINES, new Line("森".repeat(200), 0)));
        rows.set(0, new Line("§2" + "森".repeat(198), 7));
        Page full = new Page(rows);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            full.write(buffer);
            helper.assertTrue(buffer.readableBytes() < 32_600 && full.equals(Page.read(buffer)) && !buffer.isReadable(),
                    "A maximum-width Unicode page should fit in a real packet and round-trip every row and size");
        } finally {
            buffer.release();
        }
        var book = new ItemStack(ModItems.BIG_BOOK.get());
        Map<Integer, Page> pages = new LinkedHashMap<>();
        for (int index = 0; index < BigBookContent.MAX_PAGES; index++) {
            pages.put(index, full);
        }
        Header header = new Header(256, 255, false, "", "", 0, Collections.nCopies(8, Chapter.EMPTY));
        BigBookContent.update(book, header, pages, false);
        var sync = new FriendlyByteBuf(Unpooled.buffer());
        try {
            sync.writeItem(book);
            ItemStack received = sync.readItem();
            helper.assertTrue(received.is(ModItems.BIG_BOOK.get()) && !received.getTag().contains("pages")
                            && BigBookContent.bookmark(received) == 255 && full.equals(BigBookContent.page(book, 255, 0)),
                    "An oversized inventory sync should carry metadata while the real item keeps all 256 pages");
        } finally {
            sync.release();
        }
        DeskBlockEntity desk = desk(helper);
        desk.setItem(DeskBlockEntity.OPEN_BOOK, book);
        helper.assertTrue(new ItemStack(Items.DIAMOND_AXE).getDestroySpeed(desk.getBlockState()) > 1
                        && desk.getBlockState().getSoundType(helper.getLevel(), desk.getBlockPos(), null) == SoundType.WOOD,
                "A wooden writing desk should use the axe speed multiplier and wood sounds");
        CompoundTag update = desk.getUpdateTag();
        CompoundTag saved = desk.saveWithoutMetadata();
        helper.assertTrue(!update.getList("Items", Tag.TAG_COMPOUND).getCompound(0).getCompound("tag").contains("pages")
                        && saved.getList("Items", Tag.TAG_COMPOUND).getCompound(0).getCompound("tag").contains("pages")
                        && desk.getPage() == 255, "Desk sync should preserve the mesh and bookmark while disk saves retain the pages");
        helper.setBlock(DESK, Blocks.AIR);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(desk.getBlockPos()).inflate(1));
        helper.assertTrue(drops.stream().filter(drop -> drop.getItem().is(ModItems.BIG_BOOK.get())).count() == 1
                        && drops.stream().anyMatch(drop -> drop.getItem().is(ModItems.BIG_BOOK.get())
                        && full.equals(BigBookContent.page(drop.getItem(), 255, 0))),
                "Breaking a desk should drop exactly one book with its complete final page");
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blankBigBookRecipeUsesPaperAndWritableBook(GameTestHelper helper) {
        Player player = player(helper);
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        for (int slot = 0; slot < 9; slot++) {
            grid.setItem(slot, new ItemStack(slot == 4 ? Items.WRITABLE_BOOK : Items.PAPER));
        }
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(recipe.isPresent() && recipe.get().assemble(grid, helper.getLevel().registryAccess())
                        .is(ModItems.BIG_BOOK.get()), "Eight paper around a writable book should create a blank Big Book");
        helper.succeed();
    }

    private static DeskBlockEntity desk(GameTestHelper helper) {
        helper.setBlock(DESK, ModBlocks.DESK.get());
        return (DeskBlockEntity) helper.getBlockEntity(DESK);
    }

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(DESK)).add(1, 0, 0));
        return player;
    }

    private static Page line(Page page, int index, String text, int scale) {
        List<Line> lines = new ArrayList<>(page.lines());
        lines.set(index, new Line(text, scale));
        return new Page(lines);
    }
}
