package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.MarkerPoleBlock;
import art.arcane.bibliocraft.blockentity.MarkerPoleBlockEntity;
import art.arcane.bibliocraft.blockentity.TableBlockEntity;
import art.arcane.bibliocraft.item.PlumbLineItem;
import art.arcane.bibliocraft.item.TapeMeasureItem;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class MeasurementGameTests {
    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = new BlockPos(3, 1, 3);

    private MeasurementGameTests() {}

    @GameTest(template = "empty")
    public static void measurementsStayOnIndividualStacksAndUseLegacyDistances(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var first = new MeasuringPlayer(level);
        var second = new MeasuringPlayer(level);
        var tape = (TapeMeasureItem) ModItems.TAPE_MEASURE.get();
        first.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tape));
        second.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tape));
        helper.setBlock(FIRST, Blocks.STONE);
        helper.setBlock(SECOND, Blocks.STONE);
        BlockPos start = helper.absolutePos(FIRST);
        BlockPos end = helper.absolutePos(SECOND);

        use(first, start, Direction.UP);
        tape.use(level, second, InteractionHand.MAIN_HAND);
        tape.use(level, second, InteractionHand.MAIN_HAND);
        second.releaseUsingItem();
        use(second, end, Direction.EAST);
        var floor = level.getBlockState(start.above());
        var wall = level.getBlockState(end.east());
        helper.assertTrue(floor.is(ModBlocks.MARKER_POLE.get()) && floor.getValue(MarkerPoleBlock.FACE) == AttachFace.FLOOR
                        && wall.is(ModBlocks.MARKER_POLE.get()) && wall.getValue(MarkerPoleBlock.FACING) == Direction.EAST,
                "The two tapes should create independent floor and wall markers");
        helper.assertTrue(floor.getCollisionShape(level, start.above()).isEmpty(), "A measuring marker has no collision");

        var reopenedTape = ItemStack.of(first.getMainHandItem().save(new CompoundTag()));
        first.setItemInHand(InteractionHand.MAIN_HAND, reopenedTape);
        first.setPos(end.getX() + 0.5, end.getY(), end.getZ() + 0.5);
        helper.assertTrue(TapeMeasureItem.liveDistance(first, reopenedTape) == 4, "Live range should survive a saved stack");
        hit(first, end, Direction.UP);
        hit(first, end, Direction.UP);
        first.releaseUsingItem();
        helper.assertTrue(first.messages.contains(Component.translatable("message.bibliocraft.tape.east_west", 3))
                        && first.messages.contains(Component.translatable("message.bibliocraft.tape.north_south", 3)),
                "Axis mode should report each inclusive horizontal distance");
        helper.assertTrue(level.isEmptyBlock(start.above()) && level.getBlockState(end.east()).is(ModBlocks.MARKER_POLE.get()),
                "Completing one tape should remove only its own marker");
        helper.assertTrue(TapeMeasureItem.liveDistance(first, reopenedTape) == 0, "Completing closes the live distance");
        use(second, start, Direction.NORTH);
        helper.assertTrue(second.messages.contains(Component.translatable("message.bibliocraft.tape.absolute", 4))
                        && level.isEmptyBlock(end.east()), "Air use should switch only the other tape to absolute mode");
        var large = TapeMeasureItem.Offset.between(BlockPos.ZERO, new BlockPos(50000, 0, -1), Direction.UP);
        helper.assertTrue(large.distance() == 50001, "Large distances should avoid integer overflow");

        helper.setBlock(SECOND, ModBlocks.TABLE.get());
        helper.assertTrue(use(first, end, Direction.EAST) == InteractionResult.PASS
                        && use(first, end, Direction.UP) == InteractionResult.PASS,
                "The tape should leave the table's display controls available");
        helper.useBlock(SECOND, first, new BlockHitResult(Vec3.atCenterOf(end), Direction.EAST, end, false));
        helper.assertTrue(((TableBlockEntity) helper.getBlockEntity(SECOND)).yAngle() == 1,
                "Using the tape on the table's side should still rotate the display");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void markerMountsAndLootDistinguishPlacedAndTemporaryPoles(GameTestHelper helper) {
        var level = helper.getLevel();
        var pole = (MarkerPoleBlock) ModBlocks.MARKER_POLE.get();
        BlockPos origin = helper.absolutePos(FIRST);
        helper.setBlock(FIRST, Blocks.STONE);
        var player = new MeasuringPlayer(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TAPE_MEASURE.get()));
        for (Direction side : Direction.values()) {
            BlockPos marker = origin.relative(side);
            use(player, origin, side);
            var state = level.getBlockState(marker);
            var shape = state.getShape(level, marker, CollisionContext.empty());
            var axis = side.getAxis();
            helper.assertTrue(state.is(pole) && state.getValue(MarkerPoleBlock.TEMPORARY)
                            && (side.getAxisDirection() == Direction.AxisDirection.POSITIVE
                            ? shape.min(axis) < 0.001 : shape.max(axis) > 0.999),
                    "The pole base should meet the clicked block on " + side);
            var blockEntity = level.getBlockEntity(marker);
            helper.assertTrue(Block.getDrops(state, level, marker, blockEntity).isEmpty(),
                    "Temporary poles should never drop a craftable pole");
            use(player, origin, side);
            helper.assertTrue(level.isEmptyBlock(marker), "Finishing removes the marker on " + side);
        }

        var placed = pole.attachedTo(Direction.UP);
        level.setBlockAndUpdate(origin.above(), placed);
        use(player, origin, Direction.UP);
        use(player, origin, Direction.UP);
        helper.assertTrue(level.getBlockState(origin.above()) == placed,
                "Measuring on an occupied face must leave the permanent pole in place");
        helper.assertTrue(Block.getDrops(placed, level, origin.above(), level.getBlockEntity(origin.above()))
                        .stream().anyMatch(stack -> stack.is(pole.asItem())), "A placed pole still drops its block item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void markerChecksRetireSavedTokensAndRemoveUnsupportedPoles(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var player = new MeasuringPlayer(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TAPE_MEASURE.get()));
        helper.setBlock(FIRST, Blocks.STONE);
        BlockPos origin = helper.absolutePos(FIRST);
        use(player, origin, Direction.UP);
        var pole = (MarkerPoleBlockEntity) level.getBlockEntity(origin.above());
        var saved = pole.saveWithoutMetadata();
        var token = saved.getUUID("Measurement");
        pole.load(saved.copy());
        pole.checkMeasurement(level);
        helper.assertTrue(!level.isEmptyBlock(origin.above()), "A saved marker stays while its holder is offline");

        MarkerPoleBlockEntity.remove(level, origin.offset(10000, 0, 10000), token);
        helper.assertTrue(!level.isEmptyBlock(origin.above()), "Closing from an unloaded origin must not force a chunk load");
        pole.checkMeasurement(level);
        helper.assertTrue(level.isEmptyBlock(origin.above()), "A finished token removes the pole when its chunk ticks");
        use(player, origin, Direction.UP);
        use(player, origin, Direction.UP);
        level.removeBlock(origin, false);
        helper.assertTrue(level.isEmptyBlock(origin.above()), "Removing the support should remove the temporary pole");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void plumbLineMeasuresTheAdjacentColumnThroughWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var player = new MeasuringPlayer(level);
        var plumbLine = (PlumbLineItem) ModItems.PLUMB_LINE.get();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(plumbLine));
        BlockPos floor = helper.absolutePos(FIRST);
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(floor.above(), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(floor.above(2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(floor.above(3), Blocks.AIR.defaultBlockState());
        Vec3 feet = Vec3.atBottomCenterOf(floor.north().above(4));
        player.setPos(feet.x, feet.y, feet.z);
        player.setYRot(0);
        plumbLine.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.messages.contains(Component.translatable("message.bibliocraft.plumb_line.depth",
                        3, floor.getY() + 1)),
                "The plumb line should measure from the next column below the player's feet to the stone beneath water");
        helper.succeed();
    }

    private static InteractionResult use(Player player, BlockPos target, Direction face) {
        InteractionResult result = hit(player, target, face);
        player.releaseUsingItem();
        return result;
    }

    private static InteractionResult hit(Player player, BlockPos target, Direction face) {
        ItemStack held = player.getMainHandItem();
        var point = Vec3.atCenterOf(target).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, target, false));
        return held.getItem().onItemUseFirst(held, context);
    }

    private static final class MeasuringPlayer extends Player {
        private final List<Component> messages = new ArrayList<>();

        private MeasuringPlayer(ServerLevel level) {
            super(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "measurement-test"));
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }

        @Override
        public void displayClientMessage(Component component, boolean overlay) {
            messages.add(component);
        }
    }
}
