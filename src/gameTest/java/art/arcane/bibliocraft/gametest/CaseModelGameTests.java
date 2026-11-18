package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.DisplayCaseBlock;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.blockentity.DisplayCaseBlockEntity;
import art.arcane.bibliocraft.menu.DisplayCaseMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class CaseModelGameTests {
    private static final BlockPos CASE = new BlockPos(2, 1, 2);

    private CaseModelGameTests() {}

    @GameTest(template = "empty")
    public static void casePlacementHasOnlyFloorAndWallShapes(GameTestHelper helper) {
        Block displayCase = ModBlocks.CASE.get();
        Player player = helper.makeMockSurvivalPlayer();
        ItemStack stack = new ItemStack(displayCase);
        BlockPos pos = helper.absolutePos(CASE);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);

        player.setXRot(45);
        BlockState floor = displayCase.getStateForPlacement(context);
        helper.assertTrue(floor.getValue(DisplayCaseBlock.FACE) == AttachFace.FLOOR,
                "Looking down should place the floor display case");
        helper.assertTrue(floor.getCollisionShape(helper.getLevel(), pos).bounds().getYsize() == 0.5,
                "The floor case should occupy the lower half");

        player.setXRot(0);
        BlockState wall = displayCase.getStateForPlacement(context);
        helper.assertTrue(wall.getValue(DisplayCaseBlock.FACE) == AttachFace.WALL,
                "Looking straight should place the wall display case");
        helper.assertTrue(wall.getCollisionShape(helper.getLevel(), pos).bounds().getYsize() == 1,
                "The wall case should occupy the full height");
        helper.assertTrue(displayCase.getStateDefinition().getPossibleStates().size() == 16
                        && !wall.getValue(DisplayCaseBlock.OPEN) && !floor.getValue(DisplayCaseBlock.OPEN),
                "Both lid states should exist for each rotation and mount, with the lid closed by default");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void caseOpensAndConservesItemsAndLining(GameTestHelper helper) {
        helper.setBlock(CASE, ModBlocks.CASE.get());
        DisplayCaseBlockEntity display = (DisplayCaseBlockEntity) helper.getBlockEntity(CASE);
        Player player = helper.makeMockSurvivalPlayer();
        ItemStack held = new ItemStack(Items.DIAMOND_SWORD).setHoverName(Component.literal("Case exhibit"));
        player.setItemInHand(InteractionHand.MAIN_HAND, held.copy());
        click(helper, player);
        helper.assertTrue(display.isEmpty() && ItemStack.matches(held, player.getMainHandItem()),
                "The closed case must leave the display and player's held stack alone");

        player.setShiftKeyDown(true);
        click(helper, player);
        helper.assertBlockProperty(CASE, DisplayCaseBlock.OPEN, true);
        player.setShiftKeyDown(false);
        click(helper, player);
        helper.assertTrue(ItemStack.matches(held, display.getItem(DisplayCaseBlockEntity.DISPLAY_SLOT))
                        && player.getMainHandItem().isEmpty(),
                "Opening the case should allow normal direct placement");
        click(helper, player);
        helper.assertTrue(display.getItem(DisplayCaseBlockEntity.DISPLAY_SLOT).isEmpty(),
                "Clicking a stocked open case should eject its exhibit");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_CARPET, 2));
        click(helper, player);
        helper.assertTrue(display.getLiningColor() == DyeColor.RED && player.getMainHandItem().getCount() == 1,
                "An open empty case should install one carpet as lining");
        click(helper, player);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "An identical lining should leave the held carpet alone");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CYAN_CARPET, 2));
        click(helper, player);
        helper.assertTrue(display.getLiningColor() == DyeColor.CYAN && player.getMainHandItem().getCount() == 1,
                "Recoloring should install exactly one new carpet");
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.DIAMOND_SWORD, CASE, 2, 1);
            helper.assertItemEntityCountIs(Items.RED_CARPET, CASE, 2, 1);
        });
    }

    @GameTest(template = "empty")
    public static void caseKeepsItsPanelAndExhibitAcrossSaveSyncAndBreakage(GameTestHelper helper) {
        BlockState state = ModBlocks.CASE.get().defaultBlockState()
                .setValue(DisplayCaseBlock.FACE, AttachFace.WALL)
                .setValue(DisplayCaseBlock.FACING, Direction.WEST)
                .setValue(DisplayCaseBlock.OPEN, true);
        helper.setBlock(CASE, state);
        DisplayCaseBlockEntity display = (DisplayCaseBlockEntity) helper.getBlockEntity(CASE);
        BlockPos pos = display.getBlockPos();
        FurnitureMaterial material = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED,
                ResourceLocation.withDefaultNamespace("bricks"));
        CompoundTag appearance = new CompoundTag();
        material.save(appearance);
        display.load(appearance);
        ItemStack exhibit = new ItemStack(Items.AMETHYST_SHARD, 12).setHoverName(Component.literal("Case exhibit"));
        display.setItem(DisplayCaseBlockEntity.DISPLAY_SLOT, exhibit.copy());
        display.setItem(DisplayCaseBlockEntity.LINING_SLOT, new ItemStack(Items.MAGENTA_CARPET));
        var blocks = helper.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);
        var savedState = NbtUtils.readBlockState(blocks, NbtUtils.writeBlockState(state));
        var restored = (DisplayCaseBlockEntity) BlockEntity.loadStatic(pos, savedState, display.saveWithFullMetadata());
        helper.assertTrue(restored != null && savedState == state && material.equals(restored.getMaterial())
                        && ItemStack.matches(exhibit, restored.getItem(DisplayCaseBlockEntity.DISPLAY_SLOT))
                        && restored.getLiningColor() == DyeColor.MAGENTA,
                "Saving must preserve the open mount, exhibit, lining, and framed appearance");

        var client = new DisplayCaseBlockEntity(pos, savedState);
        client.handleUpdateTag(display.getUpdateTag());
        helper.assertTrue(ItemStack.matches(exhibit, client.getItem(DisplayCaseBlockEntity.DISPLAY_SLOT))
                        && client.getModelData().get(DisplayCaseBlockEntity.LINING_COLOR) == DyeColor.MAGENTA,
                "The client needs the displayed stack and updated lining sprite");

        ItemStack clone = state.getBlock().getCloneItemStack(helper.getLevel(), pos, state);
        helper.assertTrue(material.equals(FurnitureMaterial.fromItem(clone))
                        && !BlockItem.getBlockEntityData(clone).contains("Items"),
                "Pick-block should preserve the panel and leave both items in the case");
        helper.getLevel().destroyBlock(pos, true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.AMETHYST_SHARD, CASE, 2, 12);
            helper.assertItemEntityCountIs(Items.MAGENTA_CARPET, CASE, 2, 1);
            helper.assertItemEntityCountIs(ModBlocks.CASE.get().asItem(), CASE, 2, 1);
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
            helper.assertTrue(drops.stream().map(ItemEntity::getItem).anyMatch(stack -> stack.is(ModBlocks.CASE.get().asItem())
                            && material.equals(FurnitureMaterial.fromItem(stack))
                            && !BlockItem.getBlockEntityData(stack).contains("Items")),
                    "The case drop should preserve the panel and release its items separately");
        });
    }

    @GameTest(template = "empty")
    public static void caseMenuAndRedstoneFollowTheLiveCase(GameTestHelper helper) {
        helper.setBlock(CASE, ModBlocks.CASE.get().defaultBlockState().setValue(DisplayCaseBlock.OPEN, true));
        helper.setBlock(CASE.west(), Blocks.STONE);
        helper.setBlock(CASE.west(2), Blocks.REDSTONE_LAMP);
        var display = (DisplayCaseBlockEntity) helper.getBlockEntity(CASE);
        Player player = helper.makeMockSurvivalPlayer();
        Vec3 center = Vec3.atCenterOf(display.getBlockPos());
        player.setPos(center.x + 1, center.y, center.z);
        DisplayCaseMenu menu = new DisplayCaseMenu(1, player.getInventory(), display);
        helper.assertTrue(menu.stillValid(player), "An open nearby case should allow the menu");
        player.getInventory().setItem(9, new ItemStack(Items.REDSTONE_BLOCK));
        menu.quickMoveStack(player, 1);
        helper.assertTrue(display.getItem(DisplayCaseBlockEntity.DISPLAY_SLOT).is(Items.REDSTONE_BLOCK),
                "The legacy slot should receive a shift-clicked redstone block");
        for (Direction direction : Direction.values()) {
            helper.assertTrue(helper.getLevel().getSignal(display.getBlockPos(), direction) == 15,
                    "The case should output power on " + direction);
        }
        helper.assertBlockProperty(CASE.west(2), RedstoneLampBlock.LIT, true);
        player.setShiftKeyDown(true);
        click(helper, player);
        helper.assertBlockProperty(CASE, DisplayCaseBlock.OPEN, false);
        helper.assertTrue(!menu.stillValid(player), "Closing the case should invalidate another player's open menu");
        helper.assertBlockProperty(CASE.west(2), RedstoneLampBlock.LIT, true);
        helper.getLevel().destroyBlock(display.getBlockPos(), true);
        helper.succeedWhen(() -> helper.assertBlockProperty(CASE.west(2), RedstoneLampBlock.LIT, false));
    }

    @GameTest(template = "empty")
    public static void caseRecipesRetainAllWoodsAndTheBlankFrame(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        var level = helper.getLevel();
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            var slab = wood == FurnitureMaterial.Wood.FRAMED ? ModItems.FRAMING_BOARD.get()
                    : BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
            for (int slot = 0; slot < 9; slot++) {
                grid.setItem(slot, new ItemStack(slab));
            }
            grid.setItem(1, new ItemStack(Items.GLASS));
            grid.setItem(4, new ItemStack(Items.WHITE_WOOL));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack result = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(result.is(ModBlocks.CASE.get().asItem()) && FurnitureMaterial.fromItem(result).wood() == wood,
                    "The " + wood.id() + " display case recipe should preserve its material");
        }
        helper.succeed();
    }

    private static void click(GameTestHelper helper, Player player) {
        BlockPos pos = helper.absolutePos(CASE);
        helper.useBlock(CASE, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }
}
