package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.DinnerPlateBlockEntity;
import art.arcane.bibliocraft.menu.DinnerPlateMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class DinnerPlateGameTests {
    private static final BlockPos PLATE = new BlockPos(2, 1, 2);

    private DinnerPlateGameTests() {}

    @GameTest(template = "empty")
    public static void dinnerPlateServesTheHungryPlayerAndReturnsBowls(GameTestHelper helper) {
        helper.setBlock(PLATE, ModBlocks.DINNER_PLATE.get());
        var plate = (DinnerPlateBlockEntity) helper.getBlockEntity(PLATE);
        Player diner = helper.makeMockSurvivalPlayer();
        Player guest = helper.makeMockSurvivalPlayer();
        diner.getFoodData().setFoodLevel(2);
        guest.getFoodData().setFoodLevel(2);
        plate.setItem(0, new ItemStack(Items.MUSHROOM_STEW));
        plate.setItem(1, new ItemStack(Items.APPLE, 2));
        plate.setItem(2, new ItemStack(Items.BREAD, 2));

        for (int bite = 0; bite < 3; bite++) {
            click(helper, diner);
        }
        click(helper, guest);
        helper.assertTrue(plate.getItem(0).is(Items.MUSHROOM_STEW)
                        && diner.getFoodData().getFoodLevel() == 2 && guest.getFoodData().getFoodLevel() == 2,
                "Three bites from one player and one from another should leave the meal alone");
        click(helper, diner);
        helper.assertTrue(plate.getItem(0).isEmpty() && plate.getItem(1).getCount() == 1 && plate.getItem(2).getCount() == 1,
                "The fourth bite should consume one item from each occupied slot while hunger remains");
        helper.assertTrue(diner.getFoodData().getFoodLevel() == 17 && guest.getFoodData().getFoodLevel() == 2,
                "The diner should get vanilla hunger values for stew, apple, and bread without affecting the guest");
        diner.getFoodData().setFoodLevel(20);
        for (int bite = 0; bite < 4; bite++) {
            click(helper, diner);
        }
        helper.assertTrue(plate.getItem(1).getCount() == 1 && plate.getItem(2).getCount() == 1,
                "A full player should not consume anything from the plate");
        helper.succeedWhen(() -> helper.assertItemEntityCountIs(Items.BOWL, PLATE, 2, 1));
    }

    @GameTest(template = "empty")
    public static void dinnerPlateMergesFoodAndSavesDistinctStacks(GameTestHelper helper) {
        helper.setBlock(PLATE, ModBlocks.DINNER_PLATE.get());
        var plate = (DinnerPlateBlockEntity) helper.getBlockEntity(PLATE);
        Player player = helper.makeMockSurvivalPlayer();
        plate.setItem(0, new ItemStack(Items.APPLE, 60));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE, 7));
        click(helper, player);
        helper.assertTrue(plate.getItem(0).getCount() == 64 && player.getMainHandItem().getCount() == 3,
                "A partial merge should retain the remainder in the player's hand");

        ItemStack named = new ItemStack(Items.APPLE, 2).setHoverName(Component.literal("Untouched fruit"));
        player.setItemInHand(InteractionHand.MAIN_HAND, named.copy());
        click(helper, player);
        helper.assertTrue(ItemStack.matches(named, plate.getItem(1)) && player.getMainHandItem().isEmpty(),
                "An apple with a custom name should retain its own identity and slot");

        var menu = new DinnerPlateMenu(1, player.getInventory(), plate);
        player.getInventory().setItem(9, new ItemStack(Items.DIRT));
        helper.assertTrue(!plate.canPlaceItem(2, player.getInventory().getItem(9))
                        && menu.quickMoveStack(player, DinnerPlateBlockEntity.SLOT_COUNT).isEmpty(),
                "The food slot filter should apply to automation and menu transfers");
        player.getInventory().setItem(9, new ItemStack(Items.BREAD, 6));
        menu.quickMoveStack(player, DinnerPlateBlockEntity.SLOT_COUNT);
        helper.assertTrue(plate.getItem(2).getCount() == 6, "Bread should transfer to the third food slot");
        var loaded = (DinnerPlateBlockEntity) BlockEntity.loadStatic(plate.getBlockPos(), plate.getBlockState(),
                plate.saveWithFullMetadata());
        helper.assertTrue(loaded != null && ItemStack.matches(loaded.getItem(1), named) && loaded.getItem(2).getCount() == 6,
                "The plate should save its distinct food stacks and slots");
        var client = new DinnerPlateBlockEntity(plate.getBlockPos(), plate.getBlockState());
        client.handleUpdateTag(plate.getUpdateTag());
        helper.assertTrue(ItemStack.matches(client.getItem(1), named) && client.getItem(0).getCount() == 64,
                "The update packet should carry the food types and quantities for visible piles");

        helper.getLevel().destroyBlock(plate.getBlockPos(), true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.APPLE, PLATE, 2, 66);
            helper.assertItemEntityCountIs(Items.BREAD, PLATE, 2, 6);
            helper.assertItemEntityCountIs(ModBlocks.DINNER_PLATE.get().asItem(), PLATE, 2, 1);
        });
    }

    @GameTest(template = "empty")
    public static void dinnerPlateCraftsFromBothQuartzSlabs(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        var level = helper.getLevel();
        for (Item slab : new Item[]{Items.QUARTZ_SLAB, Items.SMOOTH_QUARTZ_SLAB}) {
            for (int slot = 0; slot < 3; slot++) {
                grid.setItem(slot, new ItemStack(slab));
            }
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            helper.assertTrue(recipe.assemble(grid, level.registryAccess()).is(ModBlocks.DINNER_PLATE.get().asItem()),
                    "Both legacy and existing port recipe materials should craft the dinner plate");
        }
        helper.succeed();
    }

    private static void click(GameTestHelper helper, Player player) {
        BlockPos pos = helper.absolutePos(PLATE);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0, 0.07, 0), Direction.UP, pos, false);
        helper.useBlock(PLATE, player, hit);
    }
}
