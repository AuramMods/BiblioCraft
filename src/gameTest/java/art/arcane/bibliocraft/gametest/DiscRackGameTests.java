package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.DiscRackBlock;
import art.arcane.bibliocraft.block.DiscRackMount;
import art.arcane.bibliocraft.blockentity.DiscRackBlockEntity;
import art.arcane.bibliocraft.menu.DiscRackMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class DiscRackGameTests {
    private static final BlockPos RACK = new BlockPos(2, 1, 2);

    private DiscRackGameTests() {}

    @GameTest(template = "empty")
    public static void discRackTargetsAllNineGroovesInEveryMount(GameTestHelper helper) {
        var block = ModBlocks.DISC_RACK.get();
        Player player = helper.makeMockSurvivalPlayer();
        for (DiscRackMount mount : DiscRackMount.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState state = block.defaultBlockState().setValue(DiscRackBlock.FACING, facing)
                        .setValue(DiscRackBlock.MOUNT, mount);
                helper.setBlock(RACK, state);
                var rack = (DiscRackBlockEntity) helper.getBlockEntity(RACK);
                for (int slot = 0; slot < DiscRackBlockEntity.SLOT_COUNT; slot++) {
                    ItemStack disc = new ItemStack(Items.MUSIC_DISC_13).setHoverName(Component.literal("Disc " + slot));
                    player.setItemInHand(InteractionHand.MAIN_HAND, disc.copy());
                    helper.useBlock(RACK, player, hit(helper, mount, facing, slot));
                    helper.assertTrue(ItemStack.matches(rack.getItem(slot), disc) && player.getMainHandItem().isEmpty(),
                            "The clicked groove should hold disc " + slot + " on " + mount + " facing " + facing);
                }
                rack.clearContent();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void discRackMountsFollowPlacementAndDrillsKeepTheInventory(GameTestHelper helper) {
        var block = ModBlocks.DISC_RACK.get();
        Player player = helper.makeMockSurvivalPlayer();
        BlockPos pos = helper.absolutePos(RACK);
        var side = new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false);
        var placement = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block), side);
        player.setXRot(-45);
        helper.assertTrue(block.getStateForPlacement(placement).getValue(DiscRackBlock.MOUNT) == DiscRackMount.CEILING,
                "Looking up should mount a rack on the ceiling");
        player.setXRot(45);
        helper.assertTrue(block.getStateForPlacement(placement).getValue(DiscRackBlock.MOUNT) == DiscRackMount.FLOOR,
                "Looking down should place a floor rack");
        player.setXRot(0);
        BlockState wall = block.getStateForPlacement(placement);
        helper.assertTrue(wall.getValue(DiscRackBlock.MOUNT) == DiscRackMount.WALL
                        && wall.getValue(DiscRackBlock.FACING) == Direction.EAST,
                "A wall rack should start upright with its front on the clicked side");
        helper.setBlock(RACK, wall);
        var rack = (DiscRackBlockEntity) helper.getBlockEntity(RACK);
        rack.setItem(6, new ItemStack(Items.MUSIC_DISC_CAT));

        var drill = new ItemStack(ModItems.HAND_DRILL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        var toolUse = new UseOnContext(player, InteractionHand.MAIN_HAND, side);
        helper.assertTrue(drill.getItem().onItemUseFirst(drill, toolUse).consumesAction(),
                "The hand drill should turn the upright rack horizontal");
        helper.assertBlockProperty(RACK, DiscRackBlock.MOUNT, DiscRackMount.WALL_HORIZONTAL);
        var powered = new ItemStack(ModItems.BIBLIO_DRILL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, powered);
        player.setShiftKeyDown(true);
        helper.assertTrue(powered.getItem().onItemUseFirst(powered, toolUse).consumesAction(),
                "Sneak-using the powered drill should rotate the entire rack");
        helper.assertBlockProperty(RACK, DiscRackBlock.FACING, Direction.SOUTH);
        helper.assertTrue(rack.getItem(6).is(Items.MUSIC_DISC_CAT),
                "Both drill operations should preserve rack contents and indices");

        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);
        var saved = NbtUtils.writeBlockState(helper.getBlockState(RACK));
        var restoredState = NbtUtils.readBlockState(registry, saved);
        var restored = (DiscRackBlockEntity) BlockEntity.loadStatic(pos, restoredState, rack.saveWithFullMetadata());
        helper.assertTrue(restoredState.getValue(DiscRackBlock.MOUNT) == DiscRackMount.WALL_HORIZONTAL
                        && restored != null && restored.getItem(6).is(Items.MUSIC_DISC_CAT),
                "The changed mount and contents should reload together");
        saved.getCompound("Properties").remove("mount");
        helper.assertTrue(NbtUtils.readBlockState(registry, saved).getValue(DiscRackBlock.MOUNT) == DiscRackMount.FLOOR,
                "States saved by the earlier port should retain their floor geometry");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(RACK, player, hit(helper, DiscRackMount.WALL_HORIZONTAL, Direction.SOUTH, 6));
        helper.assertTrue(rack.getItem(6).isEmpty(), "Sneak-clicking the rotated groove should eject that disc");
        helper.succeedWhen(() -> helper.assertItemEntityCountIs(Items.MUSIC_DISC_CAT, RACK, 2, 1));
    }

    @GameTest(template = "empty")
    public static void discRackMenuFiltersItemsAndDropsItsContents(GameTestHelper helper) {
        helper.setBlock(RACK, ModBlocks.DISC_RACK.get());
        var rack = (DiscRackBlockEntity) helper.getBlockEntity(RACK);
        Player player = helper.makeMockSurvivalPlayer();
        var menu = new DiscRackMenu(1, player.getInventory(), rack);
        player.getInventory().setItem(9, new ItemStack(Items.DIRT));
        helper.assertTrue(menu.quickMoveStack(player, DiscRackBlockEntity.SLOT_COUNT).isEmpty(),
                "Unrelated items should stay in the player inventory");
        player.getInventory().setItem(9, new ItemStack(ModBlocks.DINNER_PLATE.get(), 7));
        menu.quickMoveStack(player, DiscRackBlockEntity.SLOT_COUNT);
        helper.assertTrue(rack.getItem(0).getCount() == 7, "The rack should accept a stack of plates from its menu");
        rack.setItem(8, new ItemStack(Items.MUSIC_DISC_13));
        var client = new DiscRackBlockEntity(rack.getBlockPos(), rack.getBlockState());
        client.handleUpdateTag(rack.getUpdateTag());
        helper.assertTrue(client.getItem(0).getCount() == 7 && client.getItem(8).is(Items.MUSIC_DISC_13),
                "The client receives both types of display items with their slots intact");
        helper.getLevel().destroyBlock(rack.getBlockPos(), true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(ModBlocks.DISC_RACK.get().asItem(), RACK, 2, 1);
            helper.assertItemEntityCountIs(ModBlocks.DINNER_PLATE.get().asItem(), RACK, 2, 7);
            helper.assertItemEntityCountIs(Items.MUSIC_DISC_13, RACK, 2, 1);
        });
    }

    private static BlockHitResult hit(GameTestHelper helper, DiscRackMount mount, Direction facing, int slot) {
        double coordinate = 0.918 - 0.1045 * slot;
        Vec3 point = switch (mount) {
            case FLOOR -> new Vec3(coordinate, 0.2, 0.5);
            case WALL -> new Vec3(0.2, 1 - coordinate, 0.5);
            case WALL_HORIZONTAL -> new Vec3(0.2, 0.5, coordinate);
            case CEILING -> new Vec3(1 - coordinate, 0.8, 0.5);
        };
        Vec3 center = Vec3.atCenterOf(helper.absolutePos(RACK));
        Vec3 target = point.subtract(0.5, 0.5, 0.5).yRot((float) Math.toRadians(180 - facing.toYRot())).add(center);
        Direction approach = switch (mount) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            default -> facing.getClockWise();
        };
        BlockPos pos = helper.absolutePos(RACK);
        var shape = helper.getBlockState(RACK).getShape(helper.getLevel(), pos);
        BlockHitResult result = shape.clip(target.relative(approach, 1.5), target.relative(approach, -0.5), pos);
        helper.assertTrue(result != null && result.getDirection() == approach,
                "The shape should be selectable from " + approach + " in mount " + mount);
        return result;
    }
}
