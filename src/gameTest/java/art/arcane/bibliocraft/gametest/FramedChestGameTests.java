package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FramedChestBlock;
import art.arcane.bibliocraft.block.FramedChestPart;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.blockentity.FramedChestBlockEntity;
import art.arcane.bibliocraft.menu.FramedChestMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class FramedChestGameTests {
    private static final BlockPos LEFT = new BlockPos(2, 1, 2);
    private static final BlockPos RIGHT = LEFT.north();
    private static final FurnitureMaterial BRICK_FRAME =
            new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("bricks"));

    private FramedChestGameTests() {}

    @GameTest(template = "empty")
    public static void drillJoinsTheAuthoredSeamsInEveryFacing(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var manual = new ItemStack(ModItems.HAND_DRILL.get());
        var powered = new ItemStack(ModItems.BIBLIO_DRILL.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos rightPos = LEFT.relative(facing);
            var left = place(helper, LEFT, facing);
            var right = place(helper, rightPos, facing);
            player.setShiftKeyDown(true);
            drill(player, manual, right);
            drill(player, manual, left);
            helper.assertBlockProperty(LEFT, FramedChestBlock.PART, FramedChestPart.LEFT);
            helper.assertBlockProperty(rightPos, FramedChestBlock.PART, FramedChestPart.RIGHT);
            var state = left.getBlockState();
            helper.assertTrue(FramedChestBlock.partner(helper.getLevel(), left.getBlockPos(), state) == right,
                    "The chest's left model and neighbor lookup should agree for every saved facing");
            Vec3 seam = new Vec3(0.5, 0.4, 0.5).relative(facing, 0.498);
            var shape = state.getShape(helper.getLevel(), left.getBlockPos(), CollisionContext.empty());
            helper.assertTrue(shape.toAabbs().stream().anyMatch(box -> box.contains(seam)),
                    "The joined chest collision should fill the authored seam");
            drill(player, powered, left);
            drill(player, powered, right);
            helper.assertBlockProperty(LEFT, FramedChestBlock.PART, FramedChestPart.SINGLE);
            helper.assertBlockProperty(rightPos, FramedChestBlock.PART, FramedChestPart.SINGLE);

            drill(player, manual, left);
            drill(player, manual, right);
            player.setShiftKeyDown(false);
            drill(player, powered, left);
            helper.assertBlockProperty(LEFT, FramedChestBlock.FACING, facing.getClockWise());
            helper.assertBlockProperty(LEFT, FramedChestBlock.PART, FramedChestPart.SINGLE);
            helper.assertBlockProperty(rightPos, FramedChestBlock.PART, FramedChestPart.SINGLE);
            helper.setBlock(rightPos, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestLabelCountsMatchingItemsAndSyncsWithoutTheInventory(GameTestHelper helper) {
        var chest = place(helper, LEFT, Direction.NORTH);
        chest.setItem(0, new ItemStack(Items.EMERALD, 64));
        chest.setItem(1, new ItemStack(Items.GOLD_INGOT, 38));
        chest.setItem(2, new ItemStack(Items.GOLD_INGOT, 32));
        ItemStack named = new ItemStack(Items.GOLD_INGOT, 45).setHoverName(Component.literal("Other gold"));
        chest.setItem(3, named);
        helper.assertTrue(chest.getLabel().is(Items.GOLD_INGOT) && chest.getLabel().getCount() == 1
                        && !chest.getLabel().hasCustomHoverName(),
                "The label should count identical items across slots and keep other tags separate");
        chest.removeItem(2, 6);
        helper.assertTrue(chest.getLabel().is(Items.EMERALD), "A tie should use the first item in slot order");
        chest.removeItem(0, 64);
        var loaded = (FramedChestBlockEntity) BlockEntity.loadStatic(chest.getBlockPos(), chest.getBlockState(), chest.saveWithFullMetadata());
        var client = new FramedChestBlockEntity(chest.getBlockPos(), chest.getBlockState());
        var update = chest.getUpdateTag();
        client.handleUpdateTag(update);
        helper.assertTrue(loaded != null && loaded.getItem(1).getCount() == 38 && loaded.getItem(2).getCount() == 26
                        && ItemStack.matches(loaded.getItem(3), named) && ItemStack.matches(loaded.getLabel(), client.getLabel())
                        && client.getLabel().is(Items.GOLD_INGOT) && client.isEmpty() && !update.contains("Items"),
                "Reload should keep the contents and sync only the rendered label to bystanders");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void joinedMenuKeepsBothLidsOpenUntilTheLastViewerLeaves(GameTestHelper helper) {
        var left = place(helper, LEFT, Direction.NORTH);
        var right = place(helper, RIGHT, Direction.NORTH);
        var owner = helper.makeMockSurvivalPlayer();
        var guest = helper.makeMockSurvivalPlayer();
        join(owner, left, right);
        for (Player player : new Player[]{owner, guest}) {
            player.setPos(Vec3.atCenterOf(left.getBlockPos()));
        }
        left.setItem(0, new ItemStack(Items.APPLE, 3));
        right.setItem(0, new ItemStack(Items.GOLD_INGOT, 11));
        int[] sounds = new int[2];
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == helper.getLevel() && event.getSound() != null
                    && event.getPosition().distanceToSqr(Vec3.atCenterOf(left.getBlockPos()).add(0, 0, -0.5)) < 0.01) {
                if (event.getSound().value() == SoundEvents.CHEST_OPEN) {
                    sounds[0]++;
                } else if (event.getSound().value() == SoundEvents.CHEST_CLOSE) {
                    sounds[1]++;
                }
            }
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            var first = new FramedChestMenu(1, owner.getInventory(), left, right);
            var second = new FramedChestMenu(2, guest.getInventory(), left, right);
            owner.containerMenu = first;
            guest.containerMenu = second;
            helper.assertTrue(first.rows() == 6 && first.slots.size() == 90 && first.getSlot(0).getItem().is(Items.APPLE)
                            && first.getSlot(27).getItem().is(Items.GOLD_INGOT) && first.stillValid(owner),
                    "The six-row menu should present left then right inventory through either half");
            helper.assertTrue(first.quickMoveStack(owner, 27).getCount() == 11 && right.getItem(0).isEmpty(),
                    "Shift-click should remove only the corresponding half's stack");
            helper.assertTrue(left.getOpenerCount() == 2 && right.getOpenerCount() == 2 && sounds[0] == 1,
                    "Two viewers should count on both halves with one opening sound at the seam");

            var clientLeft = new FramedChestBlockEntity(left.getBlockPos(), left.getBlockState());
            clientLeft.handleUpdateTag(left.getUpdateTag());
            for (int tick = 0; tick < 11; tick++) {
                clientLeft.tickLid();
            }
            helper.assertTrue(clientLeft.openness(1) == 1, "A newly tracking client should animate the saved open state");
            first.removed(owner);
            helper.assertTrue(left.getOpenerCount() == 1 && right.getOpenerCount() == 1 && sounds[1] == 0,
                    "Closing one menu should leave the pair open for the other viewer");
            second.removed(guest);
            helper.assertTrue(left.getOpenerCount() == 0 && right.getOpenerCount() == 0 && sounds[1] == 1,
                    "The last out should close both halves with one seam sound");
            clientLeft.triggerEvent(FramedChestBlockEntity.OPEN_EVENT, 0);
            for (int tick = 0; tick < 11; tick++) {
                clientLeft.tickLid();
            }
            helper.assertTrue(clientLeft.openness(1) == 0, "The closing event should bring the lid to rest");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lockedPairRejectsCrossHalfAccessAndBreakOnlyDropsOneInventory(GameTestHelper helper) {
        var left = place(helper, LEFT, Direction.NORTH);
        var right = place(helper, RIGHT, Direction.NORTH);
        var owner = helper.makeMockSurvivalPlayer();
        var guest = helper.makeMockSurvivalPlayer();
        join(owner, left, right);
        guest.setPos(Vec3.atCenterOf(right.getBlockPos()));
        left.setItem(0, new ItemStack(Items.GOLD_INGOT, 9));
        right.setItem(0, new ItemStack(Items.DIAMOND, 17));
        var menu = new FramedChestMenu(1, guest.getInventory(), left, right);
        guest.containerMenu = menu;
        var leftHandler = left.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
        left.toggleLock(owner);
        helper.assertTrue(!menu.stillValid(guest) && leftHandler.extractItem(0, 1, false).isEmpty()
                        && right.getItem(0).getCount() == 17,
                "Locking either half should deny joined menu access and protect its own automation");
        guest.setShiftKeyDown(false);
        drill(guest, new ItemStack(ModItems.BIBLIO_DRILL.get()), right);
        helper.assertBlockProperty(RIGHT, FramedChestBlock.PART, FramedChestPart.RIGHT);
        helper.getLevel().destroyBlock(right.getBlockPos(), true);
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(LEFT, FramedChestBlock.PART, FramedChestPart.SINGLE);
            helper.assertItemEntityCountIs(ModBlocks.FRAMED_CHEST.get().asItem(), RIGHT, 2, 1);
            helper.assertItemEntityCountIs(Items.DIAMOND, RIGHT, 2, 17);
            helper.assertItemEntityCountIs(Items.GOLD_INGOT, RIGHT, 2, 0);
            helper.assertTrue(left.getItem(0).getCount() == 9 && left.isLocked(),
                    "Breaking an unlocked half must leave the other half's storage and lock intact");
        });
    }

    @GameTest(template = "empty")
    public static void chestRecipesAndDropsPreserveAllMaterialsAndTheLabelPanel(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        ItemStack customChest = ItemStack.EMPTY;
        for (FurnitureMaterial.Wood material : FurnitureMaterial.Wood.values()) {
            var wood = material == FurnitureMaterial.Wood.FRAMED ? BRICK_FRAME : new FurnitureMaterial(material, null);
            Item planks = material == FurnitureMaterial.Wood.FRAMED ? ModItems.FRAMING_SHEET.get()
                    : ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(material.id() + "_planks"));
            for (int slot = 0; slot < 9; slot++) {
                grid.setItem(slot, new ItemStack(planks));
            }
            var label = new ItemStack(ModBlocks.LABEL.get());
            grid.setItem(4, label);
            if (material != FurnitureMaterial.Wood.OAK) {
                helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(),
                        "The legacy pattern requires a matching label material");
            }
            grid.setItem(4, wood.applyTo(label));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack output = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(output.is(ModBlocks.FRAMED_CHEST.get().asItem()) && wood.equals(FurnitureMaterial.fromItem(output)),
                    "The chest should inherit the matching label's wood and any custom panel");
            if (material == FurnitureMaterial.Wood.FRAMED) {
                customChest = output;
            }
        }
        helper.setBlock(LEFT.below(), Blocks.STONE);
        BlockPos pos = helper.absolutePos(LEFT);
        player.setItemInHand(InteractionHand.MAIN_HAND, customChest);
        customChest.useOn(context(player, pos.below(), Direction.UP));
        var chest = (FramedChestBlockEntity) helper.getBlockEntity(LEFT);
        helper.assertTrue(BRICK_FRAME.equals(chest.getMaterial())
                        && BRICK_FRAME.equals(FurnitureMaterial.fromItem(chest.getBlockState().getBlock()
                                .getCloneItemStack(level, pos, chest.getBlockState()))),
                "Placement and pick-block should keep the material data");
        level.destroyBlock(pos, true);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5),
                entity -> entity.getItem().is(ModBlocks.FRAMED_CHEST.get().asItem()));
        helper.assertTrue(drops.size() == 1 && BRICK_FRAME.equals(FurnitureMaterial.fromItem(drops.get(0).getItem())),
                "The dropped chest should keep the panel independently of its contents");
        helper.succeed();
    }

    private static FramedChestBlockEntity place(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, ModBlocks.FRAMED_CHEST.get().defaultBlockState().setValue(FramedChestBlock.FACING, facing));
        return (FramedChestBlockEntity) helper.getBlockEntity(pos);
    }

    private static void join(Player player, FramedChestBlockEntity left, FramedChestBlockEntity right) {
        var tool = new ItemStack(ModItems.HAND_DRILL.get());
        player.setShiftKeyDown(true);
        drill(player, tool, left);
        drill(player, tool, right);
        player.setShiftKeyDown(false);
    }

    private static void drill(Player player, ItemStack tool, FramedChestBlockEntity chest) {
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        tool.getItem().onItemUseFirst(tool, context(player, chest.getBlockPos(), Direction.EAST));
    }

    private static UseOnContext context(Player player, BlockPos pos, Direction side) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).relative(side, 0.5), side, pos, false));
    }
}
