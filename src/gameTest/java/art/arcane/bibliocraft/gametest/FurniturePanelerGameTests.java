package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.block.FurniturePanelerBlock;
import art.arcane.bibliocraft.blockentity.FurniturePanelerBlockEntity;
import art.arcane.bibliocraft.menu.FurniturePanelerMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class FurniturePanelerGameTests {
    private static final BlockPos STATION = new BlockPos(2, 1, 2);
    private static final ResourceLocation BRICKS = ResourceLocation.withDefaultNamespace("bricks");

    private FurniturePanelerGameTests() {}

    @GameTest(template = "empty")
    public static void panelerMenuConsumesExactlyOneInputPerResult(GameTestHelper helper) {
        helper.setBlock(STATION, ModBlocks.FURNITURE_PANELER.get());
        var paneler = (FurniturePanelerBlockEntity) helper.getBlockEntity(STATION);
        Player firstPlayer = helper.makeMockSurvivalPlayer();
        Player secondPlayer = helper.makeMockSurvivalPlayer();
        paneler.setItem(FurniturePanelerBlockEntity.PANELS, new ItemStack(Items.BRICKS, 9));
        paneler.setItem(FurniturePanelerBlockEntity.FURNITURE, framedShelf());
        FurniturePanelerMenu first = new FurniturePanelerMenu(1, firstPlayer.getInventory(), paneler);
        FurniturePanelerMenu second = new FurniturePanelerMenu(2, secondPlayer.getInventory(), paneler);

        helper.assertBlockProperty(STATION, FurniturePanelerBlock.STACKS, 2);
        for (int slot = 0; slot < 36; slot++) {
            firstPlayer.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        helper.assertTrue(first.quickMoveStack(firstPlayer, FurniturePanelerMenu.RESULT).isEmpty(),
                "A full player inventory cannot collect a result");
        helper.assertTrue(paneler.getItem(FurniturePanelerBlockEntity.PANELS).getCount() == 9,
                "A failed transfer cannot consume a panel");

        firstPlayer.getInventory().clearContent();
        ItemStack taken = first.quickMoveStack(firstPlayer, FurniturePanelerMenu.RESULT);
        helper.assertTrue(taken.is(ModBlocks.SHELF.get().asItem()) && BRICKS.equals(FurnitureMaterial.fromItem(taken).panel()),
                "Shift-click should return a brick-paneled shelf");
        helper.assertBlockProperty(STATION, FurniturePanelerBlock.STACKS, 1);
        helper.assertTrue(paneler.getItem(FurniturePanelerBlockEntity.PANELS).getCount() == 8
                        && paneler.getItem(FurniturePanelerBlockEntity.FURNITURE).isEmpty(),
                "Taking a result should consume exactly one block and one piece of furniture");

        second.clicked(FurniturePanelerMenu.RESULT, 0, ClickType.PICKUP, secondPlayer);
        helper.assertTrue(second.getCarried().isEmpty(), "A second viewer cannot collect a stale result");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void panelerTopSlotsUsePersistedInputs(GameTestHelper helper) {
        helper.setBlock(STATION, ModBlocks.FURNITURE_PANELER.get());
        var paneler = (FurniturePanelerBlockEntity) helper.getBlockEntity(STATION);
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRICKS, 2));
        clickTop(helper, player, 0.75, 0.5);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "The panel stack should move into the workstation");
        player.setItemInHand(InteractionHand.MAIN_HAND, framedShelf());
        clickTop(helper, player, 0.25, 0.25);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "The framed shelf should move into the workstation");

        BlockPos pos = helper.absolutePos(STATION);
        var restored = (FurniturePanelerBlockEntity) BlockEntity.loadStatic(pos, paneler.getBlockState(),
                paneler.saveWithFullMetadata());
        helper.assertTrue(restored != null && BRICKS.equals(FurnitureMaterial.fromItem(restored.getResult()).panel()),
                "The result should be derived from its saved inputs");
        helper.assertTrue(BRICKS.equals(paneler.getModelData().get(FurniturePanelerBlockEntity.PANEL_SOURCE)),
                "The visible panels should use the same material as the result");

        player.setShiftKeyDown(true);
        clickTop(helper, player, 0.25, 0.75);
        helper.assertTrue(paneler.getResult().isEmpty()
                        && paneler.getItem(FurniturePanelerBlockEntity.PANELS).getCount() == 1,
                "Taking from the top should consume only one set of inputs");
        helper.succeedWhen(() -> {
            var nearby = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
            helper.assertTrue(nearby.stream().map(ItemEntity::getItem).anyMatch(drop ->
                            drop.is(ModBlocks.SHELF.get().asItem()) && BRICKS.equals(FurnitureMaterial.fromItem(drop).panel())),
                    "The top output should eject the paneled shelf");
        });
    }

    private static ItemStack framedShelf() {
        return new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, null).applyTo(new ItemStack(ModBlocks.SHELF.get()));
    }

    private static void clickTop(GameTestHelper helper, Player player, double x, double z) {
        BlockPos pos = helper.absolutePos(STATION);
        Vec3 hit = new Vec3(pos.getX() + x, pos.getY() + 0.63, pos.getZ() + z);
        helper.useBlock(STATION, player, new BlockHitResult(hit, Direction.UP, pos, false));
    }
}
