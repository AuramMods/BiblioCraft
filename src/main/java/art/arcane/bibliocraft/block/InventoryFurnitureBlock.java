package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.blockentity.FurnitureInventoryBlockEntity;
import art.arcane.bibliocraft.item.FurnitureBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

abstract class InventoryFurnitureBlock extends HorizontalFacingEntityBlock {
    protected InventoryFurnitureBlock(Properties properties, VoxelShape shape, String blockEntityPath) {
        super(properties, shape, blockEntityPath);
    }

    @Override
    public final InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                       InteractionHand hand, BlockHitResult hit) {
        var furniture = FurnitureBlockEntity.findAt(level, pos);
        if (furniture != null && !furniture.canAccess(player)) {
            return FurnitureBlockEntity.denyAccess(player);
        }
        return useFurniture(state, level, pos, player, hand, hit);
    }

    protected abstract InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                                       InteractionHand hand, BlockHitResult hit);

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        var furniture = FurnitureBlockEntity.findAt(level, pos);
        if (!player.isCreative() && furniture != null && furniture.isLocked()) {
            return 0;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    protected static boolean insertStack(FurnitureInventoryBlockEntity furniture, int slot, ItemStack held) {
        ItemStack stored = furniture.getItem(slot);
        if (held.isEmpty() || (!stored.isEmpty() && !ItemStack.isSameItemSameTags(stored, held))) {
            return false;
        }
        int limit = Math.min(held.getMaxStackSize(), furniture.getMaxStackSize());
        int amount = Math.min(held.getCount(), limit - stored.getCount());
        if (amount <= 0) {
            return false;
        }
        furniture.setItem(slot, held.copyWithCount(stored.getCount() + amount));
        held.shrink(amount);
        return true;
    }

    protected static boolean ejectStack(Level level, BlockPos pos, Direction opening,
                                        FurnitureInventoryBlockEntity furniture, int slot) {
        return ejectStack(level, Vec3.atCenterOf(pos).relative(opening, 0.25), furniture, slot);
    }

    protected static boolean ejectStack(Level level, Vec3 drop, FurnitureInventoryBlockEntity furniture, int slot) {
        ItemStack stored = furniture.getItem(slot);
        if (stored.isEmpty()) {
            return false;
        }
        ItemEntity entity = new ItemEntity(level, drop.x, drop.y, drop.z, stored.copy());
        entity.setDefaultPickUpDelay();
        if (!level.addFreshEntity(entity)) {
            return false;
        }
        furniture.setItem(slot, ItemStack.EMPTY);
        return true;
    }

    protected static void updatePowerNeighbors(Level level, BlockPos pos, BlockState state,
                                               @Nullable Direction unpoweredSide) {
        level.updateNeighborsAt(pos, state.getBlock());
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (direction != unpoweredSide && level.getBlockState(neighbor).isRedstoneConductor(level, neighbor)) {
                level.updateNeighborsAtExceptFromFacing(neighbor, state.getBlock(), direction.getOpposite());
            }
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (stack.getItem() instanceof FurnitureBlockItem
                && FurnitureBlockEntity.findAt(level, pos) instanceof FurnitureInventoryBlockEntity furniture) {
            return furniture.getMaterial().applyTo(stack);
        }
        return stack;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (dropsInventorySeparately() && !level.isClientSide && !state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof FurnitureInventoryBlockEntity furniture) {
            Containers.dropContents(level, pos, furniture);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    protected boolean dropsInventorySeparately() {
        return true;
    }
}
