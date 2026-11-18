package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.FurniturePanelerBlockEntity;
import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.menu.FurniturePanelerMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FurniturePanelerBlock extends InventoryFurnitureBlock {
    public static final IntegerProperty STACKS = IntegerProperty.create("panels", 0, 8);

    public FurniturePanelerBlock(Properties properties, VoxelShape shape) {
        super(properties, shape, "furniture_paneler");
        registerDefaultState(defaultBlockState().setValue(STACKS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STACKS);
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioFeatureToggles.isBlockEnabled(ModBlocks.FURNITURE_PANELER.getId())
                || !(level.getBlockEntity(pos) instanceof FurniturePanelerBlockEntity paneler)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (hit.getDirection() == Direction.UP && clickTop(paneler, state, hit, player)) {
                return InteractionResult.CONSUME;
            }
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, viewer) -> new FurniturePanelerMenu(id, inventory, paneler), getName()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean clickTop(FurniturePanelerBlockEntity paneler, BlockState state,
                                    BlockHitResult hit, Player player) {
        BlockPos pos = hit.getBlockPos();
        Vec3 local = hit.getLocation().subtract(Vec3.atCenterOf(pos))
                .yRot((state.getValue(FACING).toYRot() - 180) * Mth.DEG_TO_RAD);
        int slot = local.x > 0 ? FurniturePanelerBlockEntity.PANELS
                : local.z < 0 ? FurniturePanelerBlockEntity.FURNITURE : FurniturePanelerMenu.RESULT;

        if (player.isShiftKeyDown()) {
            boolean resultSlot = slot == FurniturePanelerMenu.RESULT;
            ItemStack stack = resultSlot ? paneler.getResult() : paneler.getItem(slot);
            if (stack.isEmpty()) {
                return false;
            }
            Level level = player.level();
            ItemEntity output = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.1,
                    pos.getZ() + 0.5, stack.copy());
            output.setDefaultPickUpDelay();
            if (!level.addFreshEntity(output)) {
                return false;
            }
            if (resultSlot) {
                paneler.consumeInputs();
            } else {
                paneler.setItem(slot, ItemStack.EMPTY);
            }
            return true;
        }

        ItemStack held = player.getMainHandItem();
        if (slot < FurniturePanelerBlockEntity.SLOT_COUNT && paneler.getItem(slot).isEmpty()
                && paneler.canPlaceItem(slot, held)) {
            int count = Math.min(held.getCount(), paneler.getMaxStackSize());
            paneler.setItem(slot, held.copyWithCount(count));
            held.shrink(count);
            return true;
        }
        return false;
    }
}
