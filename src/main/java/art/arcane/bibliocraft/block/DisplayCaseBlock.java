package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.DisplayCaseBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.DisplayCaseMenu;
import art.arcane.bibliocraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DisplayCaseBlock extends FloorWallFacingEntityBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public DisplayCaseBlock(Properties properties, VoxelShape floorShape, VoxelShape wallShape) {
        super(properties, floorShape, wallShape, "case");
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
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableWeaponcase.get()
                || !(level.getBlockEntity(pos) instanceof DisplayCaseBlockEntity displayCase)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                boolean open = !state.getValue(OPEN);
                level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS);
                level.playSound(null, pos, (open ? ModSounds.COPEN : ModSounds.CCLOSE).get(),
                        SoundSource.BLOCKS, 1, 1);
            } else if (state.getValue(OPEN)) {
                interactWithOpenCase(state, level, pos, player, displayCase);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void interactWithOpenCase(BlockState state, Level level, BlockPos pos, Player player,
                                      DisplayCaseBlockEntity displayCase) {
        Direction opening = state.getValue(FACE) == AttachFace.FLOOR ? Direction.UP : state.getValue(FACING).getClockWise();
        ItemStack held = player.getMainHandItem();
        if (!displayCase.getItem(DisplayCaseBlockEntity.DISPLAY_SLOT).isEmpty()) {
            ejectStack(level, pos, opening, displayCase, DisplayCaseBlockEntity.DISPLAY_SLOT);
        } else if (held.isEmpty()) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, viewer) -> new DisplayCaseMenu(id, inventory, displayCase), getName()));
        } else if (DisplayCaseBlockEntity.isLining(held)) {
            ItemStack lining = displayCase.getItem(DisplayCaseBlockEntity.LINING_SLOT);
            if (lining.isEmpty() || (!ItemStack.isSameItemSameTags(lining, held)
                    && ejectStack(level, pos, opening, displayCase, DisplayCaseBlockEntity.LINING_SLOT))) {
                displayCase.setItem(DisplayCaseBlockEntity.LINING_SLOT, held.split(1));
            }
        } else {
            insertStack(displayCase, DisplayCaseBlockEntity.DISPLAY_SLOT, held);
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof DisplayCaseBlockEntity displayCase && displayCase.hasRedstoneBlock()
                ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    public static void updateSignalNeighbors(Level level, BlockPos pos, BlockState state) {
        updatePowerNeighbors(level, pos, state, null);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean powered = !level.isClientSide && !state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof DisplayCaseBlockEntity displayCase && displayCase.hasRedstoneBlock();
        super.onRemove(state, level, pos, replacement, moving);
        if (powered) {
            updateSignalNeighbors(level, pos, state);
        }
    }
}
