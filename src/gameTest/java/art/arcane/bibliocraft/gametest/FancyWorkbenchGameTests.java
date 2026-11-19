package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FancyWorkbenchBlock;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.blockentity.BookcaseBlockEntity;
import art.arcane.bibliocraft.blockentity.FancyWorkbenchBlockEntity;
import art.arcane.bibliocraft.crafting.RecipeBookCrafting;
import art.arcane.bibliocraft.crafting.RecordedRecipe;
import art.arcane.bibliocraft.item.FurnitureDrillItem;
import art.arcane.bibliocraft.menu.FancyWorkbenchMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class FancyWorkbenchGameTests {
    private static final BlockPos BENCH = new BlockPos(2, 1, 2);
    private static final BlockPos LEFT = BENCH.south();
    private static final BlockPos RIGHT = BENCH.north();

    private FancyWorkbenchGameTests() {}

    @GameTest(template = "empty")
    public static void sharedShelfAndSideCasesKeepPrivateCraftingGrids(GameTestHelper helper) {
        var bench = placeBench(helper);
        helper.setBlock(LEFT, ModBlocks.BOOKCASE.get());
        helper.setBlock(RIGHT, ModBlocks.BOOKCASE.get());
        var left = (BookcaseBlockEntity) helper.getBlockEntity(LEFT);
        var right = (BookcaseBlockEntity) helper.getBlockEntity(RIGHT);
        var author = playerNear(helper);
        var visitor = playerNear(helper);
        right.toggleLock(author);
        helper.assertTrue(FancyWorkbenchBlock.sideBookcase(bench, author, true) == left
                        && FancyWorkbenchBlock.sideBookcase(bench, visitor, false) == null
                        && FancyWorkbenchBlock.sideBookcase(bench, author, false) == right,
                "Only an authorized viewer should get slots for the locked neighboring case");
        var menu = new FancyWorkbenchMenu(1, author.getInventory(), bench, left, right);
        var other = new FancyWorkbenchMenu(2, visitor.getInventory(), bench, left, null);
        menu.getSlot(FancyWorkbenchMenu.GRID_END).set(new ItemStack(Items.WRITTEN_BOOK));
        helper.assertTrue(left.getItem(0).is(Items.WRITTEN_BOOK), "The left strip should use the real bookcase inventory");
        menu.getSlot(0).set(new ItemStack(Items.BOOK));
        menu.getSlot(FancyWorkbenchMenu.GRID_START + 1).set(new ItemStack(Items.OAK_PLANKS, 4));
        menu.getSlot(FancyWorkbenchMenu.GRID_START + 4).set(new ItemStack(Items.OAK_PLANKS, 4));
        helper.assertTrue(other.getSlot(FancyWorkbenchMenu.GRID_START + 1).getItem().isEmpty()
                        && other.getSlot(FancyWorkbenchMenu.RESULT).getItem().isEmpty(),
                "Each menu should keep its own crafting grid and output");
        helper.assertTrue(menu.clickMenuButton(author, FancyWorkbenchMenu.SAVE_RECIPE),
                "A plain book should record the author's real result");
        var recorded = RecordedRecipe.read(other.getSlot(0).getItem());
        helper.assertTrue(recorded.output().is(Items.STICK) && recorded.output().getCount() == 4
                        && recorded.grid().get(1).getCount() == 1 && recorded.grid().get(4).getCount() == 1,
                "The same recorded book should be shared while the sample ingredients remain normalized");
        helper.assertTrue(count(author, Items.STICK) == 0 && menu.getSlot(FancyWorkbenchMenu.GRID_START + 1).getItem().getCount() == 4,
                "Recording a recipe should neither craft it nor consume the example stacks");
        helper.assertTrue(menu.quickMoveStack(author, FancyWorkbenchMenu.RESULT).is(Items.STICK)
                        && count(author, Items.STICK) == 4
                        && menu.getSlot(FancyWorkbenchMenu.GRID_START + 1).getItem().getCount() == 3,
                "Shift-clicking output should perform one ordinary crafting operation");
        menu.quickMoveStack(author, FancyWorkbenchMenu.GRID_START + 1);
        menu.quickMoveStack(author, FancyWorkbenchMenu.GRID_START + 4);
        helper.assertTrue(count(author, Items.OAK_PLANKS) == 6 && bench.getItem(0).is(ModItems.RECIPE_BOOK.get()),
                "Taking the unused ingredients should recover exact counts and leave the shared book in place");
        bench.toggleLock(author);
        helper.assertTrue(!other.stillValid(visitor) && !other.clickMenuButton(visitor, FancyWorkbenchMenu.LOAD_RECIPE),
                "Locking the bench should invalidate the guest menu");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recordedPatternLoadsCompleteBatchesAndPreservesConflicts(GameTestHelper helper) {
        var bench = placeBench(helper);
        var player = playerNear(helper);
        var menu = new FancyWorkbenchMenu(1, player.getInventory(), bench, null, null);
        var pattern = NonNullList.withSize(9, ItemStack.EMPTY);
        pattern.set(0, new ItemStack(Items.OAK_PLANKS));
        pattern.set(3, new ItemStack(Items.OAK_PLANKS));
        bench.setItem(0, RecordedRecipe.create(pattern, new ItemStack(Items.STICK, 4),
                ResourceLocation.withDefaultNamespace("stick")));
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 7));
        menu.getSlot(FancyWorkbenchMenu.GRID_START + 1).set(new ItemStack(Items.DIRT));
        helper.assertTrue(!menu.clickMenuButton(player, FancyWorkbenchMenu.LOAD_RECIPE)
                        && count(player, Items.OAK_PLANKS) == 7
                        && menu.getSlot(FancyWorkbenchMenu.GRID_START + 1).getItem().is(Items.DIRT),
                "A conflicting grid should keep all items in place");
        menu.getSlot(FancyWorkbenchMenu.GRID_START + 1).set(ItemStack.EMPTY);
        helper.assertTrue(menu.clickMenuButton(player, FancyWorkbenchMenu.LOAD_RECIPE)
                        && menu.getSlot(FancyWorkbenchMenu.GRID_START).getItem().getCount() == 3
                        && menu.getSlot(FancyWorkbenchMenu.GRID_START + 3).getItem().getCount() == 3
                        && count(player, Items.OAK_PLANKS) == 1 && menu.getSlot(FancyWorkbenchMenu.RESULT).getItem().is(Items.STICK),
                "Seven planks should load three complete stick recipes and leave one plank");
        menu.quickMoveStack(player, FancyWorkbenchMenu.GRID_START);
        menu.quickMoveStack(player, FancyWorkbenchMenu.GRID_START + 3);
        helper.assertTrue(count(player, Items.OAK_PLANKS) == 7, "Taking the loaded ingredients should preserve all seven planks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void portableRecipeConsumesIngredientsAndReturnsVanillaRemainders(GameTestHelper helper) {
        var player = playerNear(helper);
        var pattern = NonNullList.withSize(9, ItemStack.EMPTY);
        List<Item> ingredients = List.of(Items.MILK_BUCKET, Items.MILK_BUCKET, Items.MILK_BUCKET,
                Items.SUGAR, Items.EGG, Items.SUGAR, Items.WHEAT, Items.WHEAT, Items.WHEAT);
        for (int slot = 0; slot < ingredients.size(); slot++) {
            pattern.set(slot, new ItemStack(ingredients.get(slot)));
            player.getInventory().setItem(slot, new ItemStack(ingredients.get(slot)));
        }
        ItemStack book = RecordedRecipe.create(pattern, new ItemStack(Items.CAKE), ResourceLocation.withDefaultNamespace("cake"));
        player.getInventory().setItem(9, book);
        RecipeBookCrafting.craft(player, book);
        helper.assertTrue(count(player, Items.CAKE) == 1 && count(player, Items.BUCKET) == 3
                        && count(player, Items.MILK_BUCKET) == 0 && count(player, Items.WHEAT) == 0
                        && count(player, Items.SUGAR) == 0 && player.getInventory().getItem(9) == book,
                "Crafting cake should consume the ingredients once, return three buckets and keep the recipe book");
        player.getInventory().setItem(10, new ItemStack(Items.WHEAT, 3));
        RecipeBookCrafting.craft(player, book);
        helper.assertTrue(count(player, Items.CAKE) == 1 && count(player, Items.WHEAT) == 3,
                "An incomplete second recipe should keep ingredients and output unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void workbenchBookMeshesFollowSavedSlotsAndFacing(GameTestHelper helper) {
        var bench = placeBench(helper);
        bench.setItem(1, new ItemStack(Items.BOOK, 2));
        bench.setItem(8, new ItemStack(Items.BOOK, 3));
        var restored = (FancyWorkbenchBlockEntity) BlockEntity.loadStatic(bench.getBlockPos(), bench.getBlockState(),
                bench.saveWithFullMetadata());
        var client = new FancyWorkbenchBlockEntity(bench.getBlockPos(), bench.getBlockState());
        var update = bench.getUpdateTag();
        client.handleUpdateTag(update);
        helper.assertTrue(restored != null && restored.getItem(1).getCount() == 2 && restored.getItem(8).getCount() == 3
                        && client.getModelData().get(FancyWorkbenchBlockEntity.OCCUPIED_BOOKS) == 0b1000_0001
                        && !update.contains("Items"), "Stored stacks should persist and chunk updates should carry only their mesh mask");
        var drill = (FurnitureDrillItem) ModItems.HAND_DRILL.get();
        var player = playerNear(helper);
        var stack = new ItemStack(drill);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(true);
        BlockPos pos = bench.getBlockPos();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            helper.setBlock(BENCH, bench.getBlockState().setValue(FancyWorkbenchBlock.FACING, facing));
            var state = helper.getBlockState(BENCH);
            Direction front = FancyWorkbenchBlock.front(state);
            Vec3 left = Vec3.atCenterOf(pos).relative(front, 0.5).relative(front.getClockWise(), 0.44);
            Vec3 right = Vec3.atCenterOf(pos).relative(front, 0.5).relative(front.getCounterClockWise(), 0.44);
            helper.assertTrue(FancyWorkbenchBlock.storageSlot(state, pos,
                            new BlockHitResult(left, front, pos, false)) == 1
                            && FancyWorkbenchBlock.storageSlot(state, pos,
                            new BlockHitResult(right, front, pos, false)) == 8,
                    "The visible left and right books should map to the corresponding slots for " + facing);
            drill.onItemUseFirst(stack, new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
            helper.assertBlockProperty(BENCH, FancyWorkbenchBlock.FACING, facing.getClockWise());
        }
        helper.setBlock(BENCH, Blocks.AIR);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(drops.stream().filter(entity -> entity.getItem().is(Items.BOOK))
                .mapToInt(entity -> entity.getItem().getCount()).sum() == 5,
                "Removing the workbench should return exactly the stored books");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void matchingBookcasesCraftEveryWorkbenchMaterial(GameTestHelper helper) {
        var player = playerNear(helper);
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        for (var wood : FurnitureMaterial.Wood.values()) {
            boolean framed = wood == FurnitureMaterial.Wood.FRAMED;
            var material = new FurnitureMaterial(wood, framed ? ResourceLocation.withDefaultNamespace("bricks") : null);
            Item slab = framed ? ModItems.FRAMING_BOARD.get()
                    : BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
            grid.setItem(0, new ItemStack(Items.FEATHER));
            grid.setItem(1, new ItemStack(Items.CRAFTING_TABLE));
            grid.setItem(2, new ItemStack(Items.BLACK_DYE));
            for (int slot : new int[]{3, 5, 6, 7, 8}) {
                grid.setItem(slot, new ItemStack(slab));
            }
            grid.setItem(4, material.applyTo(new ItemStack(ModBlocks.BOOKCASE.get())));
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
            helper.assertTrue(recipe.isPresent(), "Matching workbench recipe should exist for " + wood);
            ItemStack output = recipe.get().assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(output.is(ModBlocks.FANCY_WORKBENCH.get().asItem())
                    && FurnitureMaterial.fromItem(output).equals(material), "Workbench crafting should carry " + wood);
        }
        helper.succeed();
    }

    private static FancyWorkbenchBlockEntity placeBench(GameTestHelper helper) {
        helper.setBlock(BENCH, ModBlocks.FANCY_WORKBENCH.get());
        return (FancyWorkbenchBlockEntity) helper.getBlockEntity(BENCH);
    }

    private static Player playerNear(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(BENCH)).add(1, 0, 0));
        return player;
    }

    private static int count(Player player, Item item) {
        return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }
}
