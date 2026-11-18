package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.blockentity.ShelfBlockEntity;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class FurnitureMaterialGameTests {
    private FurnitureMaterialGameTests() {}

    @GameTest(template = "empty")
    public static void shelfRecipesPreserveMatchingWoodAndFrames(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            boolean framed = wood == FurnitureMaterial.Wood.FRAMED;
            Item plank = framed ? ModItems.FRAMING_SHEET.get() : vanillaItem(wood.id() + "_planks");
            Item slab = framed ? ModItems.FRAMING_BOARD.get() : vanillaItem(wood.id() + "_slab");
            grid.setItem(4, new ItemStack(plank));
            for (int slot : new int[]{0, 1, 2, 6, 7, 8}) {
                grid.setItem(slot, new ItemStack(slab));
            }
            ItemStack shelf = craft(helper, grid);
            helper.assertTrue(shelf.is(ModBlocks.SHELF.get().asItem()),
                    "A matching " + wood.id() + " recipe should craft a shelf");
            helper.assertTrue(FurnitureMaterial.fromItem(shelf).wood() == wood,
                    "The result should carry its crafted material " + wood.id());
        }

        grid.setItem(4, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(!craft(helper, grid).is(ModBlocks.SHELF.get().asItem()),
                "A shelf cannot mix ordinary wood with framing parts");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void panelerRecipesPreserveMaterialsAndReturnTheSaw(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        grid.setItem(0, new ItemStack(Items.IRON_INGOT));
        grid.setItem(1, new ItemStack(ModItems.FRAMING_SAW.get()));
        grid.setItem(2, new ItemStack(Items.IRON_INGOT));
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            boolean framed = wood == FurnitureMaterial.Wood.FRAMED;
            Item plank = framed ? ModItems.FRAMING_SHEET.get() : vanillaItem(wood.id() + "_planks");
            Item slab = framed ? ModItems.FRAMING_BOARD.get() : vanillaItem(wood.id() + "_slab");
            for (int column = 0; column < 3; column++) {
                grid.setItem(3 + column, new ItemStack(slab));
                grid.setItem(6 + column, new ItemStack(plank));
            }
            CraftingRecipe recipe = helper.getLevel().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel()).orElseThrow();
            ItemStack result = recipe.assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(ModBlocks.FURNITURE_PANELER.get().asItem())
                            && FurnitureMaterial.fromItem(result).wood() == wood,
                    "The paneler should match the crafted " + wood.id() + " material");
            helper.assertTrue(recipe.getRemainingItems(grid).get(1).is(ModItems.FRAMING_SAW.get()),
                    "Shaping a paneler should return the saw");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void paneledShelfSurvivesPlacementSavingAndDrops(GameTestHelper helper) {
        BlockPos support = new BlockPos(2, 0, 2);
        helper.setBlock(support, Blocks.STONE);
        BlockPos floor = helper.absolutePos(support);
        BlockPos pos = floor.above();
        Player player = helper.makeMockSurvivalPlayer();

        var material = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED,
                ResourceLocation.withDefaultNamespace("bricks"));
        BlockItem shelfItem = (BlockItem) ModBlocks.SHELF.get().asItem();
        ItemStack stack = material.applyTo(new ItemStack(shelfItem));
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, floor, false);
        var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
        helper.assertTrue(shelfItem.place(context).consumesAction(), "The framed shelf should place normally");

        ShelfBlockEntity shelf = (ShelfBlockEntity) helper.getBlockEntity(support.above());
        helper.assertTrue(material.equals(shelf.getMaterial()), "Placement should transfer the panel");
        shelf.setItem(0, new ItemStack(Items.PAPER, 3));
        ItemStack clone = shelf.getBlockState().getBlock().getCloneItemStack(helper.getLevel(), pos, shelf.getBlockState());
        helper.assertTrue(material.equals(FurnitureMaterial.fromItem(clone))
                        && !BlockItem.getBlockEntityData(clone).contains("Items"),
                "Pick-block should preserve only the panel data");

        var restored = (ShelfBlockEntity) BlockEntity.loadStatic(pos, shelf.getBlockState(), shelf.saveWithFullMetadata());
        helper.assertTrue(restored != null && material.equals(restored.getMaterial()),
                "The panel should survive a world save round trip");
        var clientCopy = new ShelfBlockEntity(pos, shelf.getBlockState());
        clientCopy.handleUpdateTag(shelf.getUpdateTag());
        helper.assertTrue(material.equals(clientCopy.getModelData().get(FurnitureBlockEntity.MATERIAL)),
                "The block update should give the client model the panel");

        helper.getLevel().destroyBlock(pos, true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(drops.stream().map(ItemEntity::getItem).anyMatch(drop -> drop.is(shelfItem)
                        && material.equals(FurnitureMaterial.fromItem(drop))
                        && !BlockItem.getBlockEntityData(drop).contains("Items")),
                "The shelf drop should carry its panel without copying inventory");
        helper.assertItemEntityCountIs(Items.PAPER, support.above(), 2, 3);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void framingSawSurvivesCuttingParts(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        grid.setItem(0, new ItemStack(Items.OAK_PLANKS));
        grid.setItem(1, new ItemStack(ModItems.FRAMING_SAW.get()));
        CraftingRecipe recipe = helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel()).orElseThrow();
        helper.assertTrue(recipe.assemble(grid, helper.getLevel().registryAccess()).is(ModItems.FRAMING_SHEET.get()),
                "The normal crafting recipe should cut framing sheets");
        helper.assertTrue(recipe.getRemainingItems(grid).get(1).is(ModItems.FRAMING_SAW.get()),
                "The saw should remain in the crafting grid");
        helper.succeed();
    }

    private static Item vanillaItem(String name) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(name));
    }

    private static ItemStack craft(GameTestHelper helper, TransientCraftingContainer grid) {
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel())
                .map(recipe -> recipe.assemble(grid, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
    }
}
