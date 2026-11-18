package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.CookieJarBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.CookieJarMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CookieJarBlock extends InventoryFurnitureBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final int PULSE_TICKS = 20;

    public CookieJarBlock(Properties properties, VoxelShape shape) {
        super(properties, shape, "cookie_jar");
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableCookieJar.get()
                || !(level.getBlockEntity(pos) instanceof CookieJarBlockEntity jar)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (!state.getValue(OPEN)) {
                jar.setPulseEnd(level.getGameTime() + PULSE_TICKS);
                setOpen(state, level, pos, true);
                level.scheduleTick(pos, this, PULSE_TICKS);
            }
            player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new CookieJarMenu(id, inventory, jar),
                    getName()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN) && level.getBlockEntity(pos) instanceof CookieJarBlockEntity jar) {
            long remaining = jar.getPulseEnd() - level.getGameTime();
            if (remaining > 0) {
                // A pending tick at this location can belong to a jar replaced during its pulse.
                level.scheduleTick(pos, this, (int) Math.min(remaining, PULSE_TICKS));
                return;
            }
            jar.setPulseEnd(0);
            setOpen(state, level, pos, false);
        }
    }

    private static void setOpen(BlockState state, Level level, BlockPos pos, boolean open) {
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS);
        updatePowerNeighbors(level, pos, state, null);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(OPEN) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        super.onRemove(state, level, pos, replacement, moving);
        if (!level.isClientSide && state.getValue(OPEN) && !state.is(replacement.getBlock())) {
            updatePowerNeighbors(level, pos, state, null);
        }
    }
}
