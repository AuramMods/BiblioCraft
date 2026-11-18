package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.ArmorStandBlock;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.blockentity.ArmorStandBlockEntity;
import art.arcane.bibliocraft.menu.ArmorStandMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class ArmorStandGameTests {
    private static final BlockPos STAND = new BlockPos(2, 1, 2);
    private static final Item[] DIAMOND = {
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS
    };
    private static final Item[] IRON = {
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS
    };

    private ArmorStandGameTests() {}

    @GameTest(template = "empty")
    public static void armorStandRestoresIndividualAndPoweredEquipmentSwaps(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var stand = place(helper, player, new ItemStack(ModBlocks.ARMOR_STAND.get()));
        helper.assertBlockProperty(STAND.above(), ArmorStandBlock.HALF, DoubleBlockHalf.UPPER);
        helper.assertTrue(helper.getLevel().getBlockEntity(stand.getBlockPos().above()) == null,
                "Only the lower half should store equipment");
        for (int index = 0; index < stand.getContainerSize(); index++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DIAMOND[index]));
            clickSlot(helper, player, index);
            helper.assertTrue(stand.getItem(index).is(DIAMOND[index]) && player.getMainHandItem().isEmpty(),
                    "Clicking each body section should transfer only the matching armor");
            player.setItemSlot(ArmorStandBlockEntity.equipmentSlot(index), new ItemStack(IRON[index]));
        }
        player.setShiftKeyDown(true);
        clickSlot(helper, player, 1);
        helper.assertTrue(stand.getItem(1).is(IRON[1]) && stand.getItem(0).is(DIAMOND[0])
                        && player.getItemBySlot(ArmorStandBlockEntity.equipmentSlot(1)).is(DIAMOND[1]),
                "An unpowered sneak click should swap the chest slot alone");
        clickSlot(helper, player, 1);
        helper.setBlock(STAND.above().east(), Blocks.REDSTONE_BLOCK);
        clickSlot(helper, player, 3);
        for (int index = 0; index < stand.getContainerSize(); index++) {
            helper.assertTrue(stand.getItem(index).is(IRON[index])
                            && player.getItemBySlot(ArmorStandBlockEntity.equipmentSlot(index)).is(DIAMOND[index]),
                    "Power to either half should let one click swap the entire armor set");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armorStandMenuAndUpdateTagPreserveRealEquipment(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var stand = place(helper, player, new ItemStack(ModBlocks.ARMOR_STAND.get()));
        player.setPos(Vec3.atCenterOf(stand.getBlockPos()));
        var menu = new ArmorStandMenu(1, player.getInventory(), stand);
        helper.assertTrue(menu.slots.size() == 44 && menu.stillValid(player)
                        && menu.getSlot(0).mayPlace(new ItemStack(Items.SKELETON_SKULL))
                        && menu.getSlot(40).mayPlace(new ItemStack(Items.CARVED_PUMPKIN))
                        && !menu.getSlot(2).mayPlace(new ItemStack(Items.IRON_BOOTS)),
                "The legacy menu should expose separate stand and player equipment with the right slot filters");

        ItemStack boots = new ItemStack(Items.IRON_BOOTS);
        boots.setDamageValue(9);
        stand.setItem(3, boots.copy());
        helper.assertTrue(!menu.quickMoveStack(player, 3).isEmpty()
                        && ItemStack.matches(player.getItemBySlot(ArmorStandBlockEntity.equipmentSlot(3)), boots),
                "Shift-clicking the stand should equip available armor on the player");
        helper.assertTrue(!menu.quickMoveStack(player, 43).isEmpty() && ItemStack.matches(stand.getItem(3), boots),
                "Shift-clicking the player armor should return the original equipment to the stand");

        var reloaded = (ArmorStandBlockEntity) BlockEntity.loadStatic(stand.getBlockPos(), stand.getBlockState(), stand.saveWithFullMetadata());
        var client = new ArmorStandBlockEntity(stand.getBlockPos(), stand.getBlockState());
        client.handleUpdateTag(stand.getUpdateTag());
        helper.assertTrue(reloaded != null && ItemStack.matches(reloaded.getItem(3), boots)
                        && ItemStack.matches(client.getItem(3), boots),
                "The complete worn stack should survive a save and the render update tag");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armorStandTopSharesItsMaterialDrillLockAndOneDrop(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        Player guest = helper.makeMockSurvivalPlayer();
        var brick = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("bricks"));
        var stand = place(helper, owner, brick.applyTo(new ItemStack(ModBlocks.ARMOR_STAND.get())));
        BlockPos upper = stand.getBlockPos().above();
        var level = helper.getLevel();
        var top = level.getBlockState(upper);
        helper.assertTrue(top.is(BlockTags.MINEABLE_WITH_AXE), "Both wooden halves should mine at axe speed");
        ItemStack picked = top.getBlock().getCloneItemStack(level, upper, top);
        helper.assertTrue(brick.equals(FurnitureMaterial.fromItem(picked)), "Pick-block on the top should copy the lower material");
        ItemStack key = new ItemStack(ModItems.BIBLIO_CREATIVE_LOCK.get());
        itemUse(owner, key, upper);
        helper.assertTrue(stand.isLocked() && top.getDestroyProgress(guest, level, upper) == 0,
                "Locking the top should also protect the shared inventory and mining on either half");
        ItemStack drill = new ItemStack(ModItems.HAND_DRILL.get());
        guest.setShiftKeyDown(true);
        owner.setShiftKeyDown(true);
        itemUse(guest, drill, upper);
        helper.assertTrue(top == level.getBlockState(upper), "A guest must not rotate a locked stand from the top");
        itemUse(owner, drill, upper);
        Direction rotated = top.getValue(ArmorStandBlock.FACING).getClockWise();
        helper.assertBlockProperty(STAND, ArmorStandBlock.FACING, rotated);
        helper.assertBlockProperty(STAND.above(), ArmorStandBlock.FACING, rotated);
        itemUse(owner, key, upper);
        stand.setItem(0, new ItemStack(Items.GOLDEN_HELMET));
        level.destroyBlock(upper, true);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, STAND);
            helper.assertBlockPresent(Blocks.AIR, STAND.above());
            helper.assertItemEntityCountIs(ModBlocks.ARMOR_STAND.get().asItem(), STAND, 2, 1);
            helper.assertItemEntityCountIs(Items.GOLDEN_HELMET, STAND, 2, 1);
            helper.assertTrue(helper.getEntities(EntityType.ITEM, STAND, 2).stream()
                            .anyMatch(item -> item.getItem().is(ModBlocks.ARMOR_STAND.get().asItem())
                                    && brick.equals(FurnitureMaterial.fromItem(item.getItem()))),
                    "Breaking the upper half should drop the original block and its material once");
        });
    }

    @GameTest(template = "empty")
    public static void armorStandUpgradesOldUnpairedBlocksWithoutReplacingNeighbors(GameTestHelper helper) {
        var state = ModBlocks.ARMOR_STAND.get().defaultBlockState().setValue(ArmorStandBlock.FACING, Direction.WEST);
        helper.setBlock(STAND.below(), Blocks.STONE);
        helper.setBlock(STAND.above(), Blocks.STONE);
        Player player = helper.makeMockSurvivalPlayer();
        ItemStack stack = new ItemStack(ModBlocks.ARMOR_STAND.get());
        placeItem(helper, player, stack);
        helper.assertBlockPresent(Blocks.AIR, STAND);
        helper.assertTrue(stack.getCount() == 1, "Blocked placement should retain the player's item");
        helper.setBlock(STAND, state);
        var stand = (ArmorStandBlockEntity) helper.getBlockEntity(STAND);
        stand.setItem(1, new ItemStack(Items.ELYTRA));
        helper.runAfterDelay(3, () -> {
            helper.assertBlockPresent(Blocks.STONE, STAND.above());
            helper.setBlock(STAND.above(), Blocks.AIR);
        });
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(STAND.above(), ArmorStandBlock.HALF, DoubleBlockHalf.UPPER);
            helper.assertBlockProperty(STAND.above(), ArmorStandBlock.FACING, Direction.WEST);
            helper.assertTrue(stand.getItem(1).is(Items.ELYTRA), "Adding an upper half must preserve the old lower inventory");
        });
    }

    @GameTest(template = "empty")
    public static void armorStandRecipesRestoreAllTwelveMaterials(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        grid.setItem(1, new ItemStack(Items.STICK));
        grid.setItem(4, new ItemStack(Items.STICK));
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            var slab = wood == FurnitureMaterial.Wood.FRAMED ? ModItems.FRAMING_BOARD.get()
                    : ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
            for (int slot = 6; slot < 9; slot++) {
                grid.setItem(slot, new ItemStack(slab));
            }
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack result = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(result.is(ModBlocks.ARMOR_STAND.get().asItem()) && FurnitureMaterial.fromItem(result).wood() == wood,
                    "The legacy slab and stick pattern should retain the matching material");
        }
        helper.succeed();
    }

    private static ArmorStandBlockEntity place(GameTestHelper helper, Player player, ItemStack stack) {
        helper.setBlock(STAND.below(), Blocks.STONE);
        placeItem(helper, player, stack);
        return (ArmorStandBlockEntity) helper.getBlockEntity(STAND);
    }

    private static void placeItem(GameTestHelper helper, Player player, ItemStack stack) {
        BlockPos floor = helper.absolutePos(STAND.below());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
    }

    private static void clickSlot(GameTestHelper helper, Player player, int slot) {
        BlockPos relative = slot < 2 ? STAND.above() : STAND;
        BlockPos pos = helper.absolutePos(relative);
        helper.useBlock(relative, player, new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0, slot % 2 == 0 ? 0.75 : 0.25, 0),
                Direction.EAST, pos, false));
    }

    private static void itemUse(Player player, ItemStack item, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        item.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false)));
    }
}
