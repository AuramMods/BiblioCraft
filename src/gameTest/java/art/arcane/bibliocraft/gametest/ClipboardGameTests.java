package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.ClipboardBlock;
import art.arcane.bibliocraft.blockentity.ClipboardBlockEntity;
import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import art.arcane.bibliocraft.book.ClipboardContent;
import art.arcane.bibliocraft.book.ClipboardContent.Page;
import art.arcane.bibliocraft.book.ClipboardContent.Task;
import art.arcane.bibliocraft.menu.ClipboardMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class ClipboardGameTests {
    private static final BlockPos CENTER = new BlockPos(3, 2, 3);

    private ClipboardGameTests() {}

    @GameTest(template = "empty")
    public static void heldClipboardEditsPagesAndRejectsAReplacedStack(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        ItemStack clipboard = new ItemStack(ModItems.BIBLIO_CLIPBOARD.get());
        player.setItemInHand(InteractionHand.OFF_HAND, clipboard);
        var menu = new ClipboardMenu(1, player, InteractionHand.OFF_HAND);
        Page first = page("Tasks", 0, "Build the workshop", 2);
        menu.edit(player, 0, first);
        helper.assertTrue(ClipboardContent.page(clipboard, 0).equals(first), "The first page should save to the held item");
        helper.assertTrue(menu.visit(player, 2) == null, "An editor cannot skip an uncreated page");
        helper.assertTrue(menu.visit(player, 1).count() == 2, "The next arrow should append one page");
        Page second = page("Follow-up", 8, "Check the doors", 1);
        menu.edit(player, 1, second);
        ItemStack loaded = ItemStack.of(clipboard.save(new CompoundTag()));
        helper.assertTrue(ClipboardContent.current(loaded) == 1 && ClipboardContent.page(loaded, 0).equals(first)
                        && ClipboardContent.page(loaded, 1).equals(second),
                "Both pages, checkbox states, and the bookmark should survive saving");
        player.setItemInHand(InteractionHand.OFF_HAND, loaded);
        menu.edit(player, 1, first);
        helper.assertTrue(menu.visit(player, 1) == null && ClipboardContent.page(clipboard, 1).equals(second)
                        && ClipboardContent.page(loaded, 1).equals(second), "An old editor cannot write to either stack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deskClipboardMergesIndependentFieldsAndHonorsTheLock(GameTestHelper helper) {
        helper.setBlock(CENTER, ModBlocks.DESK.get());
        var desk = (DeskBlockEntity) helper.getBlockEntity(CENTER);
        var clipboard = new ItemStack(ModItems.BIBLIO_CLIPBOARD.get());
        helper.assertTrue(desk.canPlaceItem(DeskBlockEntity.OPEN_BOOK, clipboard), "A clipboard should fit on the desk");
        desk.setItem(DeskBlockEntity.OPEN_BOOK, clipboard);
        var first = helper.makeMockSurvivalPlayer();
        var second = helper.makeMockSurvivalPlayer();
        Vec3 center = Vec3.atCenterOf(desk.getBlockPos());
        first.setPos(center.x, center.y, center.z);
        second.setPos(center.x, center.y, center.z);
        var writer = new ClipboardMenu(1, first, desk);
        var checker = new ClipboardMenu(2, second, desk);
        writer.edit(first, 0, page("Build list", 0, "Windows", 0));
        checker.edit(second, 0, page("", 0, "", 1));
        helper.assertTrue(ClipboardContent.page(clipboard, 0).equals(page("Build list", 0, "Windows", 1)),
                "Changing a mark should keep the other editor's title and task text");
        desk.toggleLock(first);
        checker.edit(second, 0, page("Rejected", 0, "Rejected", 2));
        helper.assertTrue(!checker.stillValid(second) && writer.stillValid(first)
                        && ClipboardContent.page(clipboard, 0).equals(page("Build list", 0, "Windows", 1)),
                "Locking the desk should invalidate the guest editor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wallClipboardPlacesAndKeepsItsContentInEveryFacing(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(CENTER, Blocks.STONE);
        BlockPos support = helper.absolutePos(CENTER);
        var player = helper.makeMockSurvivalPlayer();
        var clipboard = new ItemStack(ModItems.BIBLIO_CLIPBOARD.get());
        ClipboardContent.save(clipboard, 0, page("Walls", 0, "First page", 0));
        ClipboardContent.save(clipboard, 1, page("Roof", 2, "Second page", 2));
        for (Direction front : Direction.Plane.HORIZONTAL) {
            ItemStack held = clipboard.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            player.setShiftKeyDown(true);
            var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit(support, front, 0.5, 0.5));
            helper.assertTrue(held.useOn(context).consumesAction() && held.isEmpty(), "Placing must consume one clipboard");
            BlockPos pos = support.relative(front);
            var state = level.getBlockState(pos);
            var wall = (ClipboardBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(state.getValue(ClipboardBlock.FACING).getClockWise() == front
                            && wall.display().page().title().equals("Walls"), "The board should face its player with page one");
            player.setShiftKeyDown(false);
            state.use(level, player, InteractionHand.MAIN_HAND, hit(pos, front, 0.24, 0.75));
            helper.assertTrue(wall.display().page().tasks().get(0).state() == 1, "Front clicks should update the visible checkbox");
            state.use(level, player, InteractionHand.MAIN_HAND, hit(pos, front, 0.70, 0.15));
            helper.assertTrue(wall.display().current() == 1 && wall.display().page().visibleParts() == 1 << 5,
                    "The next-page arrow should show the saved cross on task three");
            var copy = new ClipboardBlockEntity(pos, state);
            copy.load(wall.getUpdateTag());
            helper.assertTrue(copy.display().equals(wall.display()) && ClipboardContent.page(copy.clipboard(), 0)
                            .tasks().get(0).state() == 1, "The update tag should carry the active and hidden pages");
            var drops = Block.getDrops(state, level, pos, wall);
            helper.assertTrue(drops.size() == 1 && ItemStack.matches(drops.get(0), wall.clipboard())
                            && ItemStack.matches(state.getBlock().getCloneItemStack(level, pos, state), wall.clipboard()),
                    "Mining and pick-block should each return one complete clipboard");
            if (front == Direction.EAST) {
                player.setShiftKeyDown(true);
                state.use(level, player, InteractionHand.MAIN_HAND, hit(pos, front, 0.5, 0.5));
                var items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.3));
                helper.assertTrue(level.isEmptyBlock(pos) && items.size() == 1 && ItemStack.matches(items.get(0).getItem(), drops.get(0)),
                        "Crouch pickup should emit one saved clipboard and remove the board");
            } else {
                level.removeBlock(pos, false);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bothClipboardRecipesPreserveUsableItems(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        grid.setItem(0, new ItemStack(Items.BLACK_DYE));
        grid.setItem(2, new ItemStack(Items.FEATHER));
        grid.setItem(3, new ItemStack(Items.PAPER));
        grid.setItem(4, new ItemStack(Items.PAPER));
        grid.setItem(5, new ItemStack(Items.PAPER));
        grid.setItem(7, new ItemStack(Items.SPRUCE_PRESSURE_PLATE));
        var clipboard = craft(helper, grid);
        helper.assertTrue(clipboard.is(ModItems.BIBLIO_CLIPBOARD.get()), "The original layout should accept a spruce plate");
        ClipboardContent.save(clipboard, 0, page("Keep this", 7, "Stored task", 2));
        grid.clearContent();
        grid.setItem(4, clipboard);
        var packed = craft(helper, grid);
        helper.assertTrue(packed.is(ModBlocks.CLIPBOARD.get().asItem()), "The existing clipboard block item should still craft");
        helper.setBlock(CENTER, Blocks.STONE);
        var origin = helper.absolutePos(CENTER);
        player.setItemInHand(InteractionHand.MAIN_HAND, packed);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit(origin, Direction.EAST, 0.5, 0.5));
        helper.assertTrue(packed.useOn(context).consumesAction(), "The crafted block item should place normally");
        var wall = (ClipboardBlockEntity) helper.getLevel().getBlockEntity(origin.east());
        helper.assertTrue(ItemStack.matches(wall.clipboard(), clipboard), "Crafting and placing must preserve the checklist");
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, TransientCraftingContainer grid) {
        var level = helper.getLevel();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
                .map(recipe -> recipe.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static Page page(String title, int index, String text, int state) {
        var tasks = new ArrayList<>(ClipboardContent.EMPTY_PAGE.tasks());
        tasks.set(index, new Task(text, state));
        return new Page(title, tasks);
    }

    private static BlockHitResult hit(BlockPos pos, Direction front, double x, double y) {
        Vec3 point = Vec3.atCenterOf(pos).relative(front, -0.423).relative(front.getCounterClockWise(), x - 0.5)
                .add(0, y - 0.5, 0);
        return new BlockHitResult(point, front, pos, false);
    }
}
