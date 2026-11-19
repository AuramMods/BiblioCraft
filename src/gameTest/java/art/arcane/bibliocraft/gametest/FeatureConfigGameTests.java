package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class FeatureConfigGameTests {
    private FeatureConfigGameTests() {}

    @GameTest(template = "empty")
    public static void disabledFurnitureRetainsItsRegistryEntryButBlocksPlacementAndRecipes(GameTestHelper helper) {
        var config = BiblioConfig.COMMON.enableGenericshelf;
        JsonObject recipe = recipe(helper, "shelf");
        var player = helper.makeMockSurvivalPlayer();
        ItemStack shelf = new ItemStack(ModBlocks.SHELF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, shelf);
        BlockPos ground = new BlockPos(3, 1, 3);
        helper.setBlock(ground, Blocks.STONE);
        var absolute = helper.absolutePos(ground);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false);
        var action = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        boolean original = config.get();
        try {
            config.set(false);
            helper.assertTrue(!enabled(recipe) && shelf.getItem().useOn(action) == InteractionResult.FAIL
                            && shelf.getCount() == 1 && helper.getBlockState(ground.above()).isAir(),
                    "Disabled shelves should not craft or place even when an existing shelf is held");
            config.set(true);
            helper.assertTrue(enabled(recipe) && shelf.getItem().useOn(action).consumesAction(),
                    "Enabling shelves should reopen the same recipe and ordinary placement");
            helper.assertBlockPresent(ModBlocks.SHELF.get(), ground.above());
        } finally {
            config.set(original);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lockRecipeRequiresBothSettingAndItemAndUsesTheOriginalGoldPattern(GameTestHelper helper) {
        var lock = BiblioConfig.COMMON.enableLock;
        var recipeFlag = BiblioConfig.COMMON.enableLockRecipe;
        JsonObject json = recipe(helper, "biblio_creative_lock");
        ResourceLocation id = id("biblio_creative_lock");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isEmpty(),
                "The default world should have no crafting recipe for the creative lock");
        boolean originalLock = lock.get();
        boolean originalFlag = recipeFlag.get();
        try {
            recipeFlag.set(true);
            lock.set(false);
            helper.assertTrue(!enabled(json), "The recipe setting cannot re-enable a disabled lock item");
            lock.set(true);
            helper.assertTrue(enabled(json), "Both switches should enable the gold recipe");
            var recipe = (ShapedRecipe) RecipeManager.fromJson(id, json, ICondition.IContext.EMPTY);
            var player = helper.makeMockSurvivalPlayer();
            var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 2, 3);
            grid.setItem(0, new ItemStack(Items.GOLD_NUGGET));
            grid.setItem(1, new ItemStack(Items.GOLD_INGOT));
            grid.setItem(2, new ItemStack(Items.GOLD_NUGGET));
            grid.setItem(3, new ItemStack(Items.GOLD_INGOT));
            grid.setItem(5, new ItemStack(Items.GOLD_INGOT));
            var level = helper.getLevel();
            helper.assertTrue(recipe.matches(grid, level) && recipe.assemble(grid, level.registryAccess())
                            .is(ModItems.BIBLIO_CREATIVE_LOCK.get()), "The original two nuggets and three ingots must craft the lock");
        } finally {
            lock.set(originalLock);
            recipeFlag.set(originalFlag);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disabledIngredientsOnlyHideRecipesThatRequireThem(GameTestHelper helper) {
        var config = BiblioConfig.COMMON.enableFurniturePaneler;
        var framedShelf = recipe(helper, "shelf_framed");
        var press = recipe(helper, "printing_press");
        var enchantedAtlas = recipe(helper, "enchantedatlas");
        boolean original = config.get();
        try {
            config.set(false);
            helper.assertTrue(!enabled(framedShelf) && enabled(press) && enabled(enchantedAtlas),
                    "A fixed framing ingredient should gate its recipe and a vanilla alternative should remain available");
        } finally {
            config.set(original);
        }
        helper.succeed();
    }

    private static JsonObject recipe(GameTestHelper helper, String name) {
        try (var reader = helper.getLevel().getServer().getResourceManager().openAsReader(id("recipes/" + name + ".json"))) {
            return GsonHelper.parse(reader);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static boolean enabled(JsonObject recipe) {
        return CraftingHelper.processConditions(recipe, "conditions", ICondition.IContext.EMPTY);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, path);
    }
}
