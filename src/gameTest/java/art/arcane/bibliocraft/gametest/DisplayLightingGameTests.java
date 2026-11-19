package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.FurnitureInventoryBlockEntity;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class DisplayLightingGameTests {
    private static final BlockPos FIXTURE = new BlockPos(2, 2, 2);

    private DisplayLightingGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void shelfSpreadsLightAndClearsItOnDirectEjection(GameTestHelper helper) {
        encloseLight(helper);
        helper.setBlock(FIXTURE, ModBlocks.SHELF.get());
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(FIXTURE);
        var player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOWSTONE));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.25, 0.25), Direction.EAST, pos, false);
        helper.useBlock(FIXTURE, player, hit);
        helper.startSequence()
                .thenWaitUntil(() -> assertLight(helper, pos.east(), 14))
                .thenExecute(() -> {
                    helper.useBlock(FIXTURE, player, hit);
                    helper.assertTrue(((FurnitureInventoryBlockEntity) helper.getBlockEntity(FIXTURE)).isEmpty(),
                            "A second direct click should eject the glowstone");
                })
                .thenWaitUntil(() -> assertLight(helper, pos.east(), 0))
                .thenExecute(() -> helper.assertItemEntityCountIs(Items.GLOWSTONE, FIXTURE, 2, 1))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void labelLightTracksTheBrightestSlotAndTurnsOffWhenBroken(GameTestHelper helper) {
        encloseLight(helper);
        helper.setBlock(FIXTURE, ModBlocks.LABEL.get());
        var level = helper.getLevel();
        var label = (FurnitureInventoryBlockEntity) helper.getBlockEntity(FIXTURE);
        var inventory = label.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        BlockPos pos = helper.absolutePos(FIXTURE);
        inventory.insertItem(1, new ItemStack(Items.TORCH), false);
        inventory.insertItem(2, new ItemStack(Items.SEA_LANTERN), false);
        var stored = label.saveWithFullMetadata();
        var restored = (FurnitureInventoryBlockEntity) BlockEntity.loadStatic(pos, label.getBlockState(), stored);
        helper.assertTrue(restored != null && restored.displayedLight() == 15,
                "The emission should be derived from an existing saved inventory");
        helper.startSequence()
                .thenWaitUntil(() -> assertLight(helper, pos.east(), 14))
                .thenExecute(() -> helper.assertTrue(inventory.extractItem(2, 1, false).is(Items.SEA_LANTERN),
                        "Automation should remove the stronger source"))
                .thenWaitUntil(() -> assertLight(helper, pos.east(), 13))
                .thenExecute(() -> level.destroyBlock(pos, true))
                .thenWaitUntil(() -> assertLight(helper, pos.east(), 0))
                .thenExecute(() -> helper.assertItemEntityCountIs(Items.TORCH, FIXTURE, 2, 1))
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void onlyLegacyWoodenFurnitureBorrowsStandaloneBlockLight(GameTestHelper helper) {
        helper.setBlock(FIXTURE, ModBlocks.CASE.get());
        helper.setBlock(FIXTURE.east(), ModBlocks.COOKIE_JAR.get());
        helper.setBlock(FIXTURE.west(), Blocks.STONE);
        var level = helper.getLevel();
        var display = (FurnitureInventoryBlockEntity) helper.getBlockEntity(FIXTURE);
        var jar = (FurnitureInventoryBlockEntity) helper.getBlockEntity(FIXTURE.east());
        display.setItem(0, new ItemStack(ModBlocks.LAMP_IRON.get()));
        jar.setItem(0, new ItemStack(Items.GLOWSTONE));
        helper.assertTrue(display.getBlockState().getLightEmission(level, display.getBlockPos()) == 15
                        && jar.getBlockState().getLightEmission(level, jar.getBlockPos()) == 0,
                "A stored BiblioCraft lamp should illuminate wooden furniture and a cookie jar should stay dark");
        display.setItem(0, new ItemStack(ModBlocks.SHELF.get()));
        helper.assertTrue(display.displayedLight() == 0, "Wooden furniture should not relay another furniture inventory");
        helper.succeed();
    }

    private static void encloseLight(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(4, 3, 4))) {
            if (pos.getY() != 2 || pos.getX() == 0 || pos.getX() == 4 || pos.getZ() == 0 || pos.getZ() == 4) {
                helper.setBlock(pos, Blocks.STONE);
            }
        }
    }

    private static void assertLight(GameTestHelper helper, BlockPos pos, int expected) {
        int actual = helper.getLevel().getBrightness(LightLayer.BLOCK, pos);
        helper.assertTrue(actual == expected, "Expected block light " + expected + " at " + pos + " but found " + actual);
    }
}
