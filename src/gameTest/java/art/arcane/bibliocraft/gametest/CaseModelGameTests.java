package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FloorWallFacingEntityBlock;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class CaseModelGameTests {
    private CaseModelGameTests() {}

    @GameTest(template = "empty")
    public static void casePlacementHasOnlyFloorAndWallShapes(GameTestHelper helper) {
        Block displayCase = ModBlocks.CASE.get();
        Player player = helper.makeMockSurvivalPlayer();
        ItemStack stack = new ItemStack(displayCase);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);

        player.setXRot(45);
        BlockState floor = displayCase.getStateForPlacement(context);
        helper.assertTrue(floor.getValue(FloorWallFacingEntityBlock.FACE) == AttachFace.FLOOR,
                "Looking down should place the floor display case");
        helper.assertTrue(floor.getCollisionShape(helper.getLevel(), pos).bounds().getYsize() == 0.5,
                "The floor case should occupy the lower half");

        player.setXRot(0);
        BlockState wall = displayCase.getStateForPlacement(context);
        helper.assertTrue(wall.getValue(FloorWallFacingEntityBlock.FACE) == AttachFace.WALL,
                "Looking straight should place the wall display case");
        helper.assertTrue(wall.getCollisionShape(helper.getLevel(), pos).bounds().getYsize() == 1,
                "The wall case should occupy the full height");
        helper.assertTrue(displayCase.getStateDefinition().getPossibleStates().size() == 8,
                "There should be four facing states for each of the floor and wall mounts");
        helper.succeed();
    }
}
