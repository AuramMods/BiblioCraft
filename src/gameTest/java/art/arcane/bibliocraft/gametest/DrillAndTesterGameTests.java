package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.DisplayCaseBlock;
import art.arcane.bibliocraft.block.ShiftableFurnitureBlock;
import art.arcane.bibliocraft.blockentity.FurnitureInventoryBlockEntity;
import art.arcane.bibliocraft.item.DrillSelection;
import art.arcane.bibliocraft.item.PortableInventory;
import art.arcane.bibliocraft.menu.ItemTesterMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class DrillAndTesterGameTests {
    private static final BlockPos TARGET = new BlockPos(3, 2, 3);

    private DrillAndTesterGameTests() {}

    @GameTest(template = "empty")
    public static void bothDrillsMoveHitboxesAndKeepStoredStacks(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var position = helper.absolutePos(TARGET);
        var paper = new ItemStack(Items.PAPER, 3).setHoverName(Component.literal("Saved"));
        for (Block block : List.of(ModBlocks.SHELF.get(), ModBlocks.BOOKCASE.get(),
                ModBlocks.BOOKCASE_CREATIVE.get(), ModBlocks.TOOL_RACK.get(), ModBlocks.POTION_SHELF.get())) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                helper.setBlock(TARGET, block.defaultBlockState().setValue(ShiftableFurnitureBlock.FACING, facing));
                var furniture = (FurnitureInventoryBlockEntity) helper.getBlockEntity(TARGET);
                furniture.setItem(0, paper.copy());
                Vec3 original = helper.getBlockState(TARGET).getCollisionShape(helper.getLevel(), position).bounds().getCenter();
                Direction shift = block == ModBlocks.POTION_SHELF.get() ? facing.getCounterClockWise() : facing.getClockWise();
                for (int step = 1; step <= 3; step++) {
                    drill(helper, TARGET, player, step == 2 ? ModItems.BIBLIO_DRILL.get() : ModItems.HAND_DRILL.get(), false);
                    int expectedStep = step % 3;
                    var state = helper.getBlockState(TARGET);
                    helper.assertBlockProperty(TARGET, ShiftableFurnitureBlock.SHIFT, expectedStep);
                    Vec3 moved = state.getCollisionShape(helper.getLevel(), position).bounds().getCenter();
                    helper.assertTrue(moved.equals(original.relative(shift, 0.25 * expectedStep))
                                    && state.getShape(helper.getLevel(), position).bounds().getCenter().equals(moved)
                                    && ItemStack.matches(furniture.getItem(0), paper),
                            "The outline and collision must move together while preserving all stored items");
                }
                drill(helper, TARGET, player, ModItems.BIBLIO_DRILL.get(), true);
                helper.assertBlockProperty(TARGET, ShiftableFurnitureBlock.FACING, facing.getClockWise());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void caseMountAndRotationPreserveOpenLidContentsAndLock(GameTestHelper helper) {
        helper.setBlock(TARGET, ModBlocks.CASE.get().defaultBlockState().setValue(DisplayCaseBlock.OPEN, true));
        Player owner = helper.makeMockSurvivalPlayer();
        Player guest = helper.makeMockSurvivalPlayer();
        var display = (FurnitureInventoryBlockEntity) helper.getBlockEntity(TARGET);
        display.setItem(0, new ItemStack(Items.REDSTONE_BLOCK));
        display.setItem(1, new ItemStack(Items.LIME_CARPET));
        display.toggleLock(owner);
        drill(helper, TARGET, guest, ModItems.HAND_DRILL.get(), false);
        helper.assertBlockProperty(TARGET, DisplayCaseBlock.FACE, AttachFace.FLOOR);
        drill(helper, TARGET, owner, ModItems.HAND_DRILL.get(), false);
        drill(helper, TARGET, owner, ModItems.BIBLIO_DRILL.get(), true);
        helper.assertBlockProperty(TARGET, DisplayCaseBlock.FACE, AttachFace.WALL);
        helper.assertBlockProperty(TARGET, DisplayCaseBlock.FACING, Direction.EAST);
        helper.assertBlockProperty(TARGET, DisplayCaseBlock.OPEN, true);
        helper.assertTrue(display.getItem(0).is(Items.REDSTONE_BLOCK) && display.getItem(1).is(Items.LIME_CARPET)
                        && display.isLocked(), "Remounting must preserve the exhibit, lining, and lock");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaDrillingSeparatesChestsWithoutDroppingContentsAndRotatesBothDoorHalves(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var combined = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
        helper.setBlock(TARGET.east(), combined.setValue(ChestBlock.TYPE, ChestType.RIGHT));
        helper.setBlock(TARGET, combined.setValue(ChestBlock.TYPE, ChestType.LEFT));
        var left = (ChestBlockEntity) helper.getBlockEntity(TARGET);
        var right = (ChestBlockEntity) helper.getBlockEntity(TARGET.east());
        left.setItem(0, new ItemStack(Items.APPLE, 2));
        right.setItem(0, new ItemStack(Items.CARROT, 3));
        drill(helper, TARGET, player, ModItems.HAND_DRILL.get(), false);
        helper.assertBlockProperty(TARGET, ChestBlock.FACING, Direction.EAST);
        helper.assertBlockProperty(TARGET, ChestBlock.TYPE, ChestType.SINGLE);
        helper.assertBlockProperty(TARGET.east(), ChestBlock.TYPE, ChestType.SINGLE);
        helper.assertTrue(left.getItem(0).getCount() == 2 && right.getItem(0).getCount() == 3,
                "Each rotated or detached chest must keep its own contents");
        helper.assertItemEntityNotPresent(Items.APPLE, TARGET, 2);
        helper.assertItemEntityNotPresent(Items.CARROT, TARGET, 2);

        BlockPos door = TARGET.south();
        helper.setBlock(door.below(), Blocks.STONE);
        var bottom = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        helper.setBlock(door, bottom);
        helper.setBlock(door.above(), bottom.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.assertTrue(drill(helper, door, player, ModItems.BIBLIO_DRILL.get(), false) == InteractionResult.PASS,
                "An ordinary drill click should leave the door interaction to vanilla");
        drill(helper, door.above(), player, ModItems.BIBLIO_DRILL.get(), true);
        helper.assertBlockProperty(door, DoorBlock.FACING, Direction.EAST);
        helper.assertBlockProperty(door.above(), DoorBlock.FACING, Direction.EAST);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aBookcaseDrillNotifiesBothOldAndNewRedstoneNeighbors(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        helper.setBlock(TARGET, ModBlocks.BOOKCASE.get());
        for (Direction side : List.of(Direction.EAST, Direction.SOUTH)) {
            helper.setBlock(TARGET.relative(side), Blocks.STONE);
            helper.setBlock(TARGET.relative(side, 2), Blocks.REDSTONE_LAMP);
        }
        var bookcase = (FurnitureInventoryBlockEntity) helper.getBlockEntity(TARGET);
        bookcase.setItem(15, new ItemStack(ModItems.BIBLIO_RED_BOOK.get()));
        helper.assertBlockProperty(TARGET.east(2), BlockStateProperties.LIT, false);
        helper.assertBlockProperty(TARGET.south(2), BlockStateProperties.LIT, true);
        drill(helper, TARGET, player, ModItems.HAND_DRILL.get(), true);
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(TARGET.east(2), BlockStateProperties.LIT, true);
            helper.assertBlockProperty(TARGET.south(2), BlockStateProperties.LIT, false);
        });
    }

    @GameTest(template = "empty")
    public static void clearingADrillSelectionOnlyAffectsTheToolInUse(GameTestHelper helper) {
        helper.setBlock(TARGET, ModBlocks.DESK.get());
        Player player = helper.makeMockSurvivalPlayer();
        drill(helper, TARGET, player, ModItems.HAND_DRILL.get(), true);
        var drill = player.getMainHandItem();
        var copy = drill.copy();
        drill.getOrCreateTag().putString("Fixture", "keep");
        helper.assertTrue(DrillSelection.hasSelection(drill), "A first desk click should select its position");
        player.setShiftKeyDown(false);
        drill.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!DrillSelection.hasSelection(drill) && DrillSelection.hasSelection(copy)
                        && drill.getTag().getString("Fixture").equals("keep"),
                "Ordinary use should clear this drill selection and keep unrelated tags");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemTesterPersistsTheExactSampleAndReturnsItOnce(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var inventory = player.getInventory();
        var tester = new ItemStack(ModItems.TESTER_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tester);
        ItemStack sample = new ItemStack(Items.DIAMOND_AXE).setHoverName(Component.literal("Named sample"));
        sample.setDamageValue(37);
        inventory.setItem(9, sample.copy());
        var menu = new ItemTesterMenu(1, inventory, InteractionHand.MAIN_HAND);
        helper.assertTrue(tester.getMaxStackSize() == 1 && !menu.getSlot(0).mayPlace(tester)
                        && menu.quickMoveStack(player, 28).isEmpty(), "The tester must protect its held stack");
        helper.assertTrue(ItemStack.matches(menu.quickMoveStack(player, 1), sample) && inventory.getItem(9).isEmpty(),
                "Moving a sample into the tester must remove it from the player's inventory");
        ItemStack saved = ItemStack.of(tester.save(new CompoundTag()));
        player.setItemInHand(InteractionHand.MAIN_HAND, saved);
        var reopened = new ItemTesterMenu(2, inventory, InteractionHand.MAIN_HAND);
        helper.assertTrue(!menu.stillValid(player) && ItemStack.matches(reopened.storedItem(), sample)
                        && ItemStack.matches(PortableInventory.read(saved), sample),
                "Reopening must show the same item's name and damage");
        reopened.quickMoveStack(player, 0);
        reopened.quickMoveStack(player, 0);
        helper.assertTrue(PortableInventory.read(saved).isEmpty() && inventory.countItem(Items.DIAMOND_AXE) == 1,
                "Removing the sample must return it exactly once");
        helper.succeed();
    }

    private static InteractionResult drill(GameTestHelper helper, BlockPos relative, Player player, Item drill,
                                           boolean sneaking) {
        if (!player.getMainHandItem().is(drill)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(drill));
        }
        player.setShiftKeyDown(sneaking);
        BlockPos pos = helper.absolutePos(relative);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return drill.onItemUseFirst(player.getMainHandItem(), new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
}
