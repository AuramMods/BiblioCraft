package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.ShelfBlock;
import art.arcane.bibliocraft.blockentity.ShelfBlockEntity;
import art.arcane.bibliocraft.menu.ShelfMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class ShelfGameTests {
    private static final BlockPos SHELF = new BlockPos(2, 1, 2);

    private ShelfGameTests() {}

    @GameTest(template = "empty")
    public static void shelfClickTransfersStacks(GameTestHelper helper) {
        helper.setBlock(SHELF, ModBlocks.SHELF.get().defaultBlockState().setValue(ShelfBlock.FACING, Direction.NORTH));
        Player player = helper.makeMockSurvivalPlayer();
        ShelfBlockEntity shelf = (ShelfBlockEntity) helper.getBlockEntity(SHELF);
        ItemStack namedStack = new ItemStack(Items.PAPER, 60);
        namedStack.getOrCreateTag().putString("Author", "Shelf test");
        player.setItemInHand(InteractionHand.MAIN_HAND, namedStack);

        click(helper, player, 0.75, 0.75);
        helper.assertTrue(shelf.getItem(0).getCount() == 60 && player.getMainHandItem().isEmpty(),
                "The upper-left compartment should take all 60 items");

        player.setItemInHand(InteractionHand.MAIN_HAND, shelf.getItem(0).copyWithCount(8));
        click(helper, player, 0.75, 0.75);
        helper.assertTrue(shelf.getItem(0).getCount() == 64 && player.getMainHandItem().getCount() == 4,
                "Merging should keep the remainder in the player's hand");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT, 3));
        click(helper, player, 0.25, 0.25);
        helper.assertTrue(shelf.getItem(3).is(Items.CARROT) && shelf.getItem(3).getCount() == 3,
                "The lower-right compartment should receive the carrots");

        click(helper, player, 0.75, 0.75);
        helper.assertTrue(shelf.getItem(0).isEmpty(), "Picking up the paper should clear its compartment");
        helper.succeedWhen(() -> helper.assertItemEntityCountIs(Items.PAPER, SHELF, 2, 64));
    }

    @GameTest(template = "empty")
    public static void shelfHitboxesFollowTheOpening(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        BlockPos pos = helper.absolutePos(SHELF);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            helper.setBlock(SHELF, ModBlocks.SHELF.get().defaultBlockState().setValue(ShelfBlock.FACING, facing));
            Direction opening = facing.getClockWise();
            Vec3 upperLeft = Vec3.atCenterOf(pos).add(0, 0.25, 0).relative(opening.getClockWise(), 0.25);
            BlockHitResult hit = helper.getBlockState(SHELF).getShape(helper.getLevel(), pos)
                    .clip(upperLeft.relative(opening, 2), upperLeft.relative(opening, -1), pos);
            helper.assertTrue(hit != null && hit.getDirection() == opening, "The shelf must open toward " + opening);

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER));
            helper.useBlock(SHELF, player, hit);
            ShelfBlockEntity shelf = (ShelfBlockEntity) helper.getBlockEntity(SHELF);
            helper.assertTrue(shelf.getItem(0).is(Items.PAPER), "The ray should hit the upper-left compartment");
            shelf.clearContent();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shelfSavesAndDropsNamedContents(GameTestHelper helper) {
        helper.setBlock(SHELF, ModBlocks.SHELF.get());
        ShelfBlockEntity shelf = (ShelfBlockEntity) helper.getBlockEntity(SHELF);
        ItemStack namedStack = new ItemStack(Items.PAPER, 12);
        namedStack.getOrCreateTag().putString("Author", "Shelf test");
        shelf.setItem(2, namedStack);
        CompoundTag saved = shelf.saveWithFullMetadata();
        ShelfBlockEntity restored = (ShelfBlockEntity) BlockEntity.loadStatic(
                shelf.getBlockPos(), shelf.getBlockState(), saved);

        helper.assertTrue(restored != null && restored.getItem(0).isEmpty()
                        && ItemStack.matches(restored.getItem(2), namedStack),
                "World data should preserve the slot, stack count, and tag");

        ShelfBlockEntity clientShelf = new ShelfBlockEntity(shelf.getBlockPos(), shelf.getBlockState());
        clientShelf.handleUpdateTag(shelf.getUpdateTag());
        helper.assertTrue(ItemStack.matches(clientShelf.getItem(2), namedStack),
                "Chunk synchronization should include displayed items");
        clientShelf.handleUpdateTag(new CompoundTag());
        helper.assertTrue(clientShelf.isEmpty(), "An empty update should clear stale display contents");

        helper.setBlock(SHELF.above(), Blocks.STONE);
        helper.assertBlockProperty(SHELF, ShelfBlock.TOP, true);
        helper.setBlock(SHELF.above(), Blocks.AIR);
        helper.assertBlockProperty(SHELF, ShelfBlock.TOP, false);
        helper.assertTrue(shelf.getItem(2).getCount() == 12, "Removing the shelf divider should preserve items");
        helper.assertItemEntityNotPresent(Items.PAPER, SHELF, 2);

        helper.getLevel().destroyBlock(helper.absolutePos(SHELF), true);
        helper.assertItemEntityCountIs(Items.PAPER, SHELF, 2, 12);
        helper.assertItemEntityCountIs(ModBlocks.SHELF.get().asItem(), SHELF, 2, 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shelfMenuMovesItemsAndClosesWithTheBlock(GameTestHelper helper) {
        helper.setBlock(SHELF, ModBlocks.SHELF.get());
        ShelfBlockEntity shelf = (ShelfBlockEntity) helper.getBlockEntity(SHELF);
        Player player = helper.makeMockSurvivalPlayer();
        Vec3 center = Vec3.atCenterOf(helper.absolutePos(SHELF));
        player.setPos(center.x + 1, center.y, center.z);
        player.getInventory().setItem(9, new ItemStack(Items.APPLE, 21));
        ShelfMenu menu = new ShelfMenu(1, player.getInventory(), shelf);

        helper.assertTrue(menu.stillValid(player), "A player near the shelf should be able to use it");
        menu.quickMoveStack(player, ShelfBlockEntity.SLOT_COUNT);
        helper.assertTrue(shelf.getItem(0).getCount() == 21 && player.getInventory().getItem(9).isEmpty(),
                "Shift-click should move all apples onto the shelf");
        menu.quickMoveStack(player, 0);
        helper.assertTrue(shelf.isEmpty() && player.getInventory().countItem(Items.APPLE) == 21,
                "Shift-click should return all apples to the player");

        helper.setBlock(SHELF, Blocks.AIR);
        helper.assertTrue(!menu.stillValid(player), "Removing a shelf should invalidate its open menu");
        helper.succeed();
    }

    private static void click(GameTestHelper helper, Player player, double height, double depth) {
        BlockPos pos = helper.absolutePos(SHELF);
        Vec3 hit = new Vec3(pos.getX() + 0.5, pos.getY() + height, pos.getZ() + depth);
        helper.useBlock(SHELF, player, new BlockHitResult(hit, Direction.EAST, pos, false));
    }
}
