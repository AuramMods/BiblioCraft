package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.DiscRackBlock;
import art.arcane.bibliocraft.block.DiscRackMount;
import art.arcane.bibliocraft.block.ShelfBlock;
import art.arcane.bibliocraft.blockentity.DiscRackBlockEntity;
import art.arcane.bibliocraft.blockentity.DisplayCaseBlockEntity;
import art.arcane.bibliocraft.blockentity.ShelfBlockEntity;
import art.arcane.bibliocraft.menu.ShelfMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandlerModifiable;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class FurnitureLockGameTests {
    private static final BlockPos FURNITURE = new BlockPos(2, 1, 2);

    private FurnitureLockGameTests() {}

    @GameTest(template = "empty")
    public static void lockedShelfRejectsVisitorsWithTheSameNameAndInvalidatesTheirMenu(GameTestHelper helper) {
        helper.setBlock(FURNITURE, ModBlocks.SHELF.get().defaultBlockState().setValue(ShelfBlock.FACING, Direction.NORTH));
        var shelf = (ShelfBlockEntity) helper.getBlockEntity(FURNITURE);
        Player owner = helper.makeMockSurvivalPlayer();
        Player guest = helper.makeMockSurvivalPlayer();
        BlockPos pos = shelf.getBlockPos();
        guest.setPos(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5);
        owner.setPos(pos.getX() + 2, pos.getY(), pos.getZ() + 0.5);
        var menu = new ShelfMenu(1, guest.getInventory(), shelf);
        helper.assertTrue(menu.stillValid(guest), "A nearby visitor should be able to open an unlocked shelf");

        ItemStack key = new ItemStack(ModItems.BIBLIO_CREATIVE_LOCK.get());
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.25, 0.25), Direction.EAST, pos, false);
        useItem(owner, key, hit);
        helper.assertTrue(shelf.isLocked() && shelf.canAccess(owner) && !shelf.canAccess(guest) && !menu.stillValid(guest),
                "Only the owner UUID should match, and an open visitor menu should become invalid");
        helper.assertTrue(owner.getGameProfile().getName().equals(guest.getGameProfile().getName()),
                "The mock players should exercise owner checks even when their names match");
        useItem(guest, key, hit);
        helper.assertTrue(shelf.isLocked() && key.getCount() == 1, "The visitor's reusable key should leave the lock intact");

        guest.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        helper.useBlock(FURNITURE, guest, hit);
        helper.assertTrue(shelf.isEmpty() && guest.getMainHandItem().is(Items.DIAMOND),
                "A denied visitor should be unable to insert into the clicked compartment");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        helper.useBlock(FURNITURE, owner, hit);
        helper.assertTrue(shelf.getItem(0).is(Items.DIAMOND) && owner.getMainHandItem().isEmpty(),
                "The owner should still be able to use a locked shelf normally");

        var saved = shelf.saveWithFullMetadata();
        var reloaded = (ShelfBlockEntity) BlockEntity.loadStatic(pos, shelf.getBlockState(), saved);
        var client = new ShelfBlockEntity(pos, shelf.getBlockState());
        client.handleUpdateTag(shelf.getUpdateTag());
        helper.assertTrue(reloaded != null && reloaded.canAccess(owner) && !reloaded.canAccess(guest)
                        && client.isLocked() && !client.canAccess(guest),
                "The owner should survive a save and update packet");
        ItemStack picked = shelf.getBlockState().getBlock().getCloneItemStack(helper.getLevel(), pos, shelf.getBlockState());
        var pickedData = BlockItem.getBlockEntityData(picked);
        helper.assertTrue(pickedData == null || !pickedData.contains("LockOwner"),
                "Ordinary pick-block should copy the material only");
        useItem(owner, key, hit);
        client.handleUpdateTag(shelf.getUpdateTag());
        helper.assertTrue(!shelf.isLocked() && !client.isLocked() && menu.stillValid(guest),
                "Unlocking should clear the persisted owner, reopen access, and replicate to the client");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aLockStopsMiningAndNonOwnerDrillActions(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(FURNITURE);
        helper.setBlock(FURNITURE, ModBlocks.DISC_RACK.get().defaultBlockState().setValue(DiscRackBlock.MOUNT, DiscRackMount.WALL));
        var rack = (DiscRackBlockEntity) helper.getBlockEntity(FURNITURE);
        Player owner = helper.makeMockSurvivalPlayer();
        Player guest = helper.makeMockSurvivalPlayer();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false);
        var key = new ItemStack(ModItems.BIBLIO_CREATIVE_LOCK.get());
        var drill = new ItemStack(ModItems.HAND_DRILL.get());
        useItem(owner, key, hit);
        var wall = rack.getBlockState();
        helper.assertTrue(wall.getDestroyProgress(owner, helper.getLevel(), pos) == 0
                        && wall.getDestroyProgress(guest, helper.getLevel(), pos) == 0,
                "Survival players should need to unlock a rack before they can mine it");
        helper.assertTrue(MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(helper.getLevel(), pos, wall, guest)),
                "The server should refuse a break request on a locked rack");

        useItem(guest, drill, hit);
        helper.assertBlockProperty(FURNITURE, DiscRackBlock.MOUNT, DiscRackMount.WALL);
        useItem(owner, drill, hit);
        helper.assertBlockProperty(FURNITURE, DiscRackBlock.MOUNT, DiscRackMount.WALL_HORIZONTAL);
        useItem(owner, key, hit);
        helper.assertTrue(!MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(helper.getLevel(), pos, rack.getBlockState(), owner)),
                "The server should allow normal mining after the lock is removed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lockedFurnitureStopsHoppersAndCachedForgeHandlers(GameTestHelper helper) {
        helper.setBlock(FURNITURE, ModBlocks.SHELF.get());
        helper.setBlock(FURNITURE.below(), Blocks.HOPPER);
        var shelf = (ShelfBlockEntity) helper.getBlockEntity(FURNITURE);
        var hopper = (HopperBlockEntity) helper.getBlockEntity(FURNITURE.below());
        var handler = shelf.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(AssertionError::new);
        var unsided = shelf.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
        shelf.setItem(0, new ItemStack(Items.IRON_INGOT, 3));
        Player owner = helper.makeMockSurvivalPlayer();
        var key = new ItemStack(ModItems.BIBLIO_CREATIVE_LOCK.get());
        BlockPos pos = shelf.getBlockPos();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false);
        useItem(owner, key, hit);
        ItemStack incoming = new ItemStack(Items.GOLD_INGOT, 2);
        helper.assertTrue(handler.extractItem(0, 1, false).isEmpty()
                        && ItemStack.matches(unsided.insertItem(1, incoming, false), incoming),
                "A lock should stop both sided and unsided handlers obtained before it was applied");
        ((IItemHandlerModifiable) unsided).setStackInSlot(0, ItemStack.EMPTY);
        helper.assertTrue(shelf.getItem(0).getCount() == 3, "A cached mutable wrapper should also honor the lock");
        helper.assertTrue(!HopperBlockEntity.suckInItems(helper.getLevel(), hopper)
                        && ItemStack.matches(HopperBlockEntity.addItem(hopper, shelf, incoming, Direction.DOWN), incoming),
                "A real hopper should fail to pull or push items through a locked shelf");

        useItem(owner, key, hit);
        helper.assertTrue(HopperBlockEntity.suckInItems(helper.getLevel(), hopper)
                        && hopper.getItem(0).is(Items.IRON_INGOT) && shelf.getItem(0).getCount() == 2,
                "The hopper should regain access to the shelf after unlocking");
        helper.assertTrue(handler.insertItem(1, incoming, false).isEmpty() && shelf.getItem(1).getCount() == 2,
                "A cached Forge handler should regain access after unlocking");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void automatedCaseInsertionPaysForOneLining(GameTestHelper helper) {
        helper.setBlock(FURNITURE, ModBlocks.CASE.get());
        var display = (DisplayCaseBlockEntity) helper.getBlockEntity(FURNITURE);
        var handler = display.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
        var leftOver = handler.insertItem(DisplayCaseBlockEntity.LINING_SLOT, new ItemStack(Items.BLUE_CARPET, 7), false);
        helper.assertTrue(leftOver.getCount() == 6 && display.getItem(DisplayCaseBlockEntity.LINING_SLOT).getCount() == 1,
                "A pipe should install exactly one new carpet and keep the remainder");
        helper.succeed();
    }

    private static void useItem(Player player, ItemStack stack, BlockHitResult hit) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().onItemUseFirst(stack, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
}
