package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.DeskBellBlockEntity;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class DeskBellBlock extends HorizontalFacingEntityBlock {
    public DeskBellBlock(Properties properties, VoxelShape shape) {
        super(properties, shape, "bell");
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableDeskBell.get()
                || !(level.getBlockEntity(pos) instanceof DeskBellBlockEntity bell)) {
            return InteractionResult.PASS;
        }
        if (!bell.canAccess(player)) {
            return FurnitureBlockEntity.denyAccess(player);
        }
        if (!level.isClientSide) {
            ring(level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!player.isCreative() && level.getBlockEntity(pos) instanceof DeskBellBlockEntity bell && bell.isLocked()) {
            return 0;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.BELL.get()) {
            return null;
        }
        return (world, pos, currentState, entity) -> ((DeskBellBlockEntity) entity).tick(world, pos);
    }

    public static void ring(Level level, BlockPos pos) {
        level.playSound(null, pos, ModSounds.DING.get(), SoundSource.BLOCKS, 1, 1);
    }
}
