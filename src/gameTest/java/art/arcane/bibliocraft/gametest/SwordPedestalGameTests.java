package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.ColorFacingEntityBlock;
import art.arcane.bibliocraft.block.SwordPedestalBlock;
import art.arcane.bibliocraft.blockentity.SwordPedestalBlockEntity;
import art.arcane.bibliocraft.item.ColoredBlockItem;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class SwordPedestalGameTests {
    private static final BlockPos PEDESTAL = new BlockPos(2, 1, 2);

    private SwordPedestalGameTests() {}

    @GameTest(template = "empty")
    public static void swordPedestalTransfersAndPowersThroughStone(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(PEDESTAL, ModBlocks.SWORD_PEDESTAL.get());
        helper.setBlock(PEDESTAL.above(), Blocks.STONE);
        helper.setBlock(PEDESTAL.above(2), Blocks.REDSTONE_LAMP);
        var pedestal = (SwordPedestalBlockEntity) helper.getBlockEntity(PEDESTAL);
        BlockPos pos = pedestal.getBlockPos();
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD).setHoverName(Component.literal("Gallery Blade"));
        sword.setDamageValue(12);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
        helper.useBlock(PEDESTAL, player, hit(pos));
        helper.assertTrue(pedestal.isEmpty() && player.getMainHandItem().is(Items.DIAMOND_AXE),
                "An axe should remain in the player's hand");
        player.setItemInHand(InteractionHand.MAIN_HAND, sword.copy());
        helper.useBlock(PEDESTAL, player, hit(pos));
        helper.assertTrue(ItemStack.matches(sword, pedestal.getItem(0)) && player.getMainHandItem().isEmpty(),
                "Right-click should transfer a sword with its damage and name intact");
        helper.assertTrue(pedestal.getBlockState().getShape(level, pos, CollisionContext.empty()).bounds().maxY == 1.0,
                "The occupied pedestal should be targetable at the sword's height");
        helper.assertBlockProperty(PEDESTAL.above(2), RedstoneLampBlock.LIT, true);
        for (Direction direction : Direction.values()) {
            helper.assertTrue(level.getSignal(pos, direction) == 15 && level.getDirectSignal(pos, direction) == 15,
                    "The occupied pedestal should power every face");
        }
        var client = new SwordPedestalBlockEntity(pos, pedestal.getBlockState());
        client.handleUpdateTag(pedestal.getUpdateTag());
        helper.assertTrue(ItemStack.matches(sword, client.getItem(0)), "The synced sword needs its display tags");

        helper.useBlock(PEDESTAL, player, hit(pos));
        helper.assertTrue(pedestal.isEmpty() && level.getSignal(pos, Direction.UP) == 0,
                "Taking the sword should clear the slot and power");
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(PEDESTAL.above(2), RedstoneLampBlock.LIT, false);
            helper.assertItemEntityCountIs(Items.DIAMOND_SWORD, PEDESTAL, 2, 1);
        });
    }

    @GameTest(template = "empty")
    public static void swordPedestalKeepsColorAndDropsInventorySeparately(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(PEDESTAL.below(), Blocks.STONE);
        Player player = helper.makeMockSurvivalPlayer();
        ItemStack placed = ColoredBlockItem.applyColor(new ItemStack(ModBlocks.SWORD_PEDESTAL.get()), DyeColor.CYAN);
        player.setItemInHand(InteractionHand.MAIN_HAND, placed);
        placed.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper.absolutePos(PEDESTAL.below()))));
        helper.assertBlockProperty(PEDESTAL, ColorFacingEntityBlock.COLOR, DyeColor.CYAN);
        var pedestal = (SwordPedestalBlockEntity) helper.getBlockEntity(PEDESTAL);
        BlockPos pos = pedestal.getBlockPos();
        var handler = pedestal.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(AssertionError::new);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        helper.assertTrue(handler.getSlotLimit(0) == 1
                        && handler.insertItem(0, new ItemStack(Items.BREAD), false).is(Items.BREAD)
                        && handler.insertItem(0, new ItemStack(ModBlocks.SWORD_PEDESTAL.get()), false).is(ModBlocks.SWORD_PEDESTAL.get().asItem())
                        && handler.insertItem(0, sword.copy(), false).isEmpty(),
                "The one-item slot should accept swords, reject the pedestal itself, and reject unrelated items");
        helper.assertTrue(handler.extractItem(0, 1, false).is(Items.IRON_SWORD) && level.getSignal(pos, Direction.UP) == 0,
                "Automation should turn the signal off when it takes the sword");
        handler.insertItem(0, sword.copy(), false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.YELLOW_DYE, 2));
        helper.useBlock(PEDESTAL, player, hit(pos));
        helper.assertBlockProperty(PEDESTAL, ColorFacingEntityBlock.COLOR, DyeColor.YELLOW);
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && pedestal.getItem(0).is(Items.IRON_SWORD),
                "Dyeing a displayed sword's pedestal should consume one dye and keep the sword in place");

        Direction initialFacing = pedestal.getBlockState().getValue(SwordPedestalBlock.FACING);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HAND_DRILL.get()));
        player.getMainHandItem().onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
        helper.assertBlockProperty(PEDESTAL, SwordPedestalBlock.FACING, initialFacing.getClockWise());
        var loaded = (SwordPedestalBlockEntity) BlockEntity.loadStatic(pos, pedestal.getBlockState(), pedestal.saveWithFullMetadata());
        helper.assertTrue(loaded != null && ItemStack.matches(loaded.getItem(0), sword), "Save data should retain the sword");
        ItemStack pick = pedestal.getBlockState().getBlock().getCloneItemStack(level, pos, pedestal.getBlockState());
        helper.assertTrue(ColoredBlockItem.getColor(pick) == DyeColor.YELLOW && pick.getTagElement("BlockEntityTag") == null,
                "Pick-block should retain the trim and leave the inventory in the original block");

        level.destroyBlock(pos, true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(ModBlocks.SWORD_PEDESTAL.get().asItem(), PEDESTAL, 2, 1);
            helper.assertTrue(helper.getEntities(EntityType.ITEM, PEDESTAL, 2).stream()
                            .map(entity -> entity.getItem()).anyMatch(stack -> stack.is(ModBlocks.SWORD_PEDESTAL.get().asItem())
                                    && ColoredBlockItem.getColor(stack) == DyeColor.YELLOW),
                    "A dropped pedestal should retain its trim");
            helper.assertItemEntityCountIs(Items.IRON_SWORD, PEDESTAL, 2, 1);
        });
    }

    @GameTest(template = "empty")
    public static void swordPedestalCraftsAllTrimColors(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        var level = helper.getLevel();
        for (int slot : new int[]{1, 3, 5}) {
            grid.setItem(slot, new ItemStack(Items.SMOOTH_STONE_SLAB));
        }
        for (DyeColor color : DyeColor.values()) {
            var wool = ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(color.getSerializedName() + "_wool"));
            grid.setItem(4, new ItemStack(wool));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack crafted = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(crafted.is(ModBlocks.SWORD_PEDESTAL.get().asItem()) && ColoredBlockItem.getColor(crafted) == color,
                    "Matching wool should craft a pedestal with " + color + " trim");
        }
        var smallGrid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 2, 2);
        for (DyeColor color : DyeColor.values()) {
            DyeColor original = color == DyeColor.RED ? DyeColor.BLUE : DyeColor.RED;
            ItemStack input = ColoredBlockItem.applyColor(new ItemStack(ModBlocks.SWORD_PEDESTAL.get()), original)
                    .setHoverName(Component.literal("Gallery Pedestal"));
            smallGrid.setItem(3, input);
            smallGrid.setItem(0, new ItemStack(DyeItem.byColor(color)));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, smallGrid, level).orElseThrow();
            ItemStack recolored = recipe.assemble(smallGrid, level.registryAccess());
            helper.assertTrue(ColoredBlockItem.getColor(recolored) == color && recolored.hasCustomHoverName()
                            && ColoredBlockItem.getColor(input) == original,
                    "Dye should produce a recolored copy and retain the pedestal's name");
            smallGrid.setItem(3, recolored);
            helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, smallGrid, level).isEmpty(),
                    "The same color should not consume a dye");
        }
        helper.succeed();
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0, 1, 0), Direction.UP, pos, false);
    }
}
