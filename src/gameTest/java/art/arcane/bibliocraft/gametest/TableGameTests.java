package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.block.TableBlock;
import art.arcane.bibliocraft.blockentity.TableBlockEntity;
import art.arcane.bibliocraft.menu.TableMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class TableGameTests {
    private static final BlockPos TABLE = new BlockPos(2, 1, 2);

    private TableGameTests() {}

    @GameTest(template = "empty")
    public static void tableConnectionsFollowTheAuthoredCornersInEveryFacing(GameTestHelper helper) {
        var block = ModBlocks.TABLE.get();
        var base = block.defaultBlockState();
        helper.setBlock(TABLE, base);
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(TABLE);
        var alone = base.getCollisionShape(level, pos, CollisionContext.empty());
        helper.assertTrue(alone.toAabbs().stream().anyMatch(box -> box.contains(0.5, 0.5, 0.5)),
                "A single table should have the legacy center post");
        helper.assertTrue(base.isFaceSturdy(level, pos, Direction.UP), "The tabletop should support blocks and wiring above it");
        helper.assertTrue(TableBlock.visibleParts(base, false, false) == bits(0, 5, 6, 7, 8),
                "A standalone table should render the center post and all four beveled quarters");

        var east = base.setValue(BlockStateProperties.EAST, true);
        helper.assertTrue(TableBlock.visibleParts(east, true, true) == bits(3, 4, 7, 8, 9, 10, 15, 16, 17, 18, 22, 23, 29),
                "An east seam should square the two east corners and retain draping cloth on the exposed edges");
        helper.assertTrue(east.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.SOUTH),
                "Moving a table in a rotated structure should also rotate the saved world connections");
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var shape = east.setValue(TableBlock.FACING, facing).getCollisionShape(level, pos, CollisionContext.empty());
            helper.assertTrue(shape.toAabbs().stream().noneMatch(box -> box.contains(0.8, 0.5, 0.2))
                            && shape.toAabbs().stream().anyMatch(box -> box.contains(0.2, 0.5, 0.2)),
                    "Every model rotation should omit the east legs and retain the outer west legs");
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(TABLE.relative(direction), base);
        }
        helper.startSequence().thenIdle(4).thenExecute(() -> {
            var center = level.getBlockState(pos);
            helper.assertTrue(TableBlock.visibleParts(center, true, false) == bits(9, 10, 11, 12, 17, 18, 19, 20),
                    "The center of a large tabletop should have only square top and cloth quarters");
            helper.assertBlockProperty(TABLE.east(), BlockStateProperties.WEST, true);
            helper.setBlock(TABLE.east(), Blocks.AIR);
        }).thenIdle(2).thenExecute(() -> {
            helper.assertBlockProperty(TABLE, BlockStateProperties.EAST, false);
            helper.assertTrue((TableBlock.visibleParts(level.getBlockState(pos), true, false) & bits(21, 24)) == bits(21, 24),
                    "Removing a neighbor should uncover both drapes along the newly exposed edge");
        }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void tableClickAndMenuPreserveStacksAndSeparateBothFabrics(GameTestHelper helper) {
        var table = placeDefault(helper);
        Player player = helper.makeMockSurvivalPlayer();
        click(helper, player, Direction.UP, new ItemStack(Items.COOKED_BEEF, 18));
        helper.assertTrue(player.getMainHandItem().isEmpty() && table.getItem(TableBlockEntity.DISPLAY).getCount() == 18,
                "A top click should display the full held stack");
        click(helper, player, Direction.UP, new ItemStack(Items.CYAN_CARPET, 4));
        helper.assertTrue(player.getMainHandItem().getCount() == 3, "Placing a tabletop cloth should consume one carpet");
        click(helper, player, Direction.NORTH, new ItemStack(Items.RED_CARPET, 2));
        helper.assertTrue(player.getMainHandItem().getCount() == 1
                        && table.getModelData().get(TableBlockEntity.CLOTH_COLOR) == DyeColor.CYAN
                        && table.getModelData().get(TableBlockEntity.CARPET_COLOR) == DyeColor.RED,
                "A side click should apply an independently colored carpet under the table");
        player.setPos(Vec3.atCenterOf(table.getBlockPos()));
        var menu = new TableMenu(1, player.getInventory(), table);
        helper.assertTrue(menu.slots.size() == 37 && menu.stillValid(player)
                        && menu.quickMoveStack(player, 0).getCount() == 18 && table.getItem(TableBlockEntity.DISPLAY).isEmpty()
                        && table.getItem(TableBlockEntity.CLOTH).getCount() == 1 && table.getItem(TableBlockEntity.CARPET).getCount() == 1,
                "The original one-slot menu should move the full display stack and leave the fabrics in place");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tableDrillsRemoveClothAndCarpetAndRotateBothAxes(GameTestHelper helper) {
        var table = placeDefault(helper);
        Player player = helper.makeMockSurvivalPlayer();
        var handDrill = new ItemStack(ModItems.HAND_DRILL.get());
        var poweredDrill = new ItemStack(ModItems.BIBLIO_DRILL.get());
        table.setItem(TableBlockEntity.DISPLAY, new ItemStack(Items.IRON_SWORD));
        table.setItem(TableBlockEntity.CLOTH, new ItemStack(Items.YELLOW_CARPET));
        table.setItem(TableBlockEntity.CARPET, new ItemStack(Items.BLUE_CARPET));
        itemUse(player, handDrill, table, Direction.SOUTH);
        itemUse(player, poweredDrill, table, Direction.EAST);
        click(helper, player, Direction.WEST, new ItemStack(ModItems.TAPE_MEASURE.get()));
        helper.assertTrue(table.xAngle() == 1 && table.yAngle() == 2, "North/south and east/west clicks should rotate independent axes");

        itemUse(player, handDrill, table, Direction.UP);
        helper.assertTrue(table.getItem(TableBlockEntity.CLOTH).isEmpty() && table.getItem(TableBlockEntity.CARPET).is(Items.BLUE_CARPET),
                "Drilling the top should eject only the tablecloth");
        player.setShiftKeyDown(true);
        itemUse(player, poweredDrill, table, Direction.NORTH);
        helper.assertTrue(table.getItem(TableBlockEntity.CARPET).isEmpty() && table.getItem(TableBlockEntity.DISPLAY).is(Items.IRON_SWORD),
                "A sneak-drill should eject only the under-table carpet");
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.YELLOW_CARPET, TABLE, 2, 1);
            helper.assertItemEntityCountIs(Items.BLUE_CARPET, TABLE, 2, 1);
        });
    }

    @GameTest(template = "empty")
    public static void tableSavesAndSyncsMapFabricAndItemRotation(GameTestHelper helper) {
        var table = placeDefault(helper);
        var level = helper.getLevel();
        BlockPos pos = table.getBlockPos();
        var state = table.getBlockState();
        ItemStack map = MapItem.create(level, pos.getX(), pos.getZ(), (byte) 0, true, false);
        table.setItem(TableBlockEntity.DISPLAY, map);
        table.setItem(TableBlockEntity.CLOTH, new ItemStack(Items.LIME_CARPET));
        table.setItem(TableBlockEntity.CARPET, new ItemStack(Items.MAGENTA_CARPET));
        table.rotateDisplay(Direction.NORTH);
        table.rotateDisplay(Direction.EAST);
        var saved = (TableBlockEntity) BlockEntity.loadStatic(pos, state, table.saveWithFullMetadata());
        var client = new TableBlockEntity(pos, state);
        client.handleUpdateTag(table.getUpdateTag());
        helper.assertTrue(saved != null && ItemStack.matches(saved.getItem(TableBlockEntity.DISPLAY), map)
                        && ItemStack.matches(client.getItem(TableBlockEntity.DISPLAY), map)
                        && MapItem.getSavedData(saved.getItem(TableBlockEntity.DISPLAY), level) != null
                        && client.xAngle() == 1 && client.yAngle() == 1
                        && client.getModelData().get(TableBlockEntity.CLOTH_COLOR) == DyeColor.LIME
                        && client.getModelData().get(TableBlockEntity.CARPET_COLOR) == DyeColor.MAGENTA,
                "A map should retain its vanilla identity and both fabrics and angles should survive reload and synchronization");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tableRecipesAndDropsKeepAllMaterialsAndContents(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            var slab = wood == FurnitureMaterial.Wood.FRAMED ? ModItems.FRAMING_BOARD.get()
                    : ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
            var plank = wood == FurnitureMaterial.Wood.FRAMED ? ModItems.FRAMING_SHEET.get()
                    : ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(wood.id() + "_planks"));
            for (int slot = 0; slot < 3; slot++) {
                grid.setItem(slot, new ItemStack(slab));
            }
            grid.setItem(4, new ItemStack(plank));
            grid.setItem(7, new ItemStack(plank));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack result = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(result.is(ModBlocks.TABLE.get().asItem()) && FurnitureMaterial.fromItem(result).wood() == wood,
                    "Each matching slab and plank material should craft the original table pattern");
        }

        helper.setBlock(TABLE.below(), Blocks.STONE);
        var frame = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("bricks"));
        var frameItem = frame.applyTo(new ItemStack(ModBlocks.TABLE.get()));
        player.setItemInHand(InteractionHand.MAIN_HAND, frameItem);
        BlockPos pos = helper.absolutePos(TABLE);
        frameItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos.below(), Direction.UP)));
        var table = (TableBlockEntity) helper.getBlockEntity(TABLE);
        ItemStack picked = level.getBlockState(pos).getBlock().getCloneItemStack(level, pos, table.getBlockState());
        helper.assertTrue(frame.equals(table.getMaterial()) && frame.equals(FurnitureMaterial.fromItem(picked)),
                "Ordinary placement and pick-block should retain a framed table's custom panel");
        table.setItem(TableBlockEntity.DISPLAY, new ItemStack(Items.DIAMOND, 7));
        table.setItem(TableBlockEntity.CLOTH, new ItemStack(Items.LIME_CARPET));
        table.setItem(TableBlockEntity.CARPET, new ItemStack(Items.MAGENTA_CARPET));
        level.destroyBlock(pos, true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.DIAMOND, TABLE, 2, 7);
            helper.assertItemEntityCountIs(Items.LIME_CARPET, TABLE, 2, 1);
            helper.assertItemEntityCountIs(Items.MAGENTA_CARPET, TABLE, 2, 1);
            helper.assertItemEntityCountIs(ModBlocks.TABLE.get().asItem(), TABLE, 2, 1);
            helper.assertTrue(helper.getEntities(EntityType.ITEM, TABLE, 2).stream()
                            .anyMatch(item -> item.getItem().is(ModBlocks.TABLE.get().asItem())
                                    && frame.equals(FurnitureMaterial.fromItem(item.getItem()))),
                    "The table's own loot should preserve its panel and return contents separately");
        });
    }

    private static TableBlockEntity placeDefault(GameTestHelper helper) {
        helper.setBlock(TABLE, ModBlocks.TABLE.get());
        return (TableBlockEntity) helper.getBlockEntity(TABLE);
    }

    private static void click(GameTestHelper helper, Player player, Direction side, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        helper.useBlock(TABLE, player, hit(helper.absolutePos(TABLE), side));
    }

    private static void itemUse(Player player, ItemStack tool, TableBlockEntity table, Direction side) {
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        tool.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(table.getBlockPos(), side)));
    }

    private static BlockHitResult hit(BlockPos pos, Direction side) {
        return new BlockHitResult(Vec3.atCenterOf(pos).relative(side, 0.5), side, pos, false);
    }

    private static int bits(int... indexes) {
        int mask = 0;
        for (int index : indexes) {
            mask |= 1 << index;
        }
        return mask;
    }
}
