package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

public abstract class SignDisplayBlock extends WoodenFurnitureBlock {
    protected SignDisplayBlock(Properties properties, VoxelShape shape, String id) {
        super(properties, shape, id);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The saved north model has its back against the west edge of the cell.
        Direction front = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, front.getCounterClockWise());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected final InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                                   InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioFeatureToggles.isBlockEnabled(ForgeRegistries.BLOCKS.getKey(this))) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(menuAt(level, pos), Component.translatable(getDescriptionId()));
            openMenu(serverPlayer, provider, level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    protected void openMenu(ServerPlayer player, MenuProvider provider, Level level, BlockPos pos) {
        NetworkHooks.openScreen(player, provider);
    }

    protected abstract MenuConstructor menuAt(Level level, BlockPos pos);
}
