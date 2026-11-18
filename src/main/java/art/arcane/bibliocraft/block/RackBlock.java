package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.RackBlockEntity;
import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.menu.RackMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

public class RackBlock extends InventoryFurnitureBlock {
    private final RackType type;

    public RackBlock(Properties properties, VoxelShape shape, String id, RackType type) {
        super(properties, shape, id);
        this.type = type;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction opening = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, type.facingForOpening(opening));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction opening = type.opening(state.getValue(FACING));
        return state.setValue(FACING, type.facingForOpening(mirror.mirror(opening)));
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND
                || !BiblioFeatureToggles.isBlockEnabled(ForgeRegistries.BLOCKS.getKey(this))
                || !(level.getBlockEntity(pos) instanceof RackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && !interactWithSlot(level, pos, type.opening(state.getValue(FACING)), player, hit, rack)) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, viewer) -> new RackMenu(id, inventory, rack), getName()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean interactWithSlot(Level level, BlockPos pos, Direction opening, Player player,
                                     BlockHitResult hit, RackBlockEntity rack) {
        Direction side = hit.getDirection();
        boolean sneaking = player.isShiftKeyDown();
        var held = player.getMainHandItem();
        if (type == RackType.TOOLS) {
            if (side != opening || sneaking) {
                return false;
            }
            int slot = slotAt(pos, hit.getLocation(), opening);
            if (!rack.canPlaceItem(slot, held) || !insertStack(rack, slot, held)) {
                ejectStack(level, pos, opening, rack, slot);
            }
            return true;
        }
        if (side != opening && side != opening.getOpposite()) {
            return false;
        }
        int slot = slotAt(pos, hit.getLocation(), opening);
        return (sneaking && ejectStack(level, pos, side, rack, slot))
                || (rack.canPlaceItem(slot, held) && insertStack(rack, slot, held));
    }

    private int slotAt(BlockPos pos, Vec3 hit, Direction opening) {
        Direction right = opening.getCounterClockWise();
        double offset = (hit.x - pos.getX() - 0.5) * right.getStepX()
                + (hit.z - pos.getZ() - 0.5) * right.getStepZ();
        int column = Mth.clamp(Mth.floor((offset + 0.5) * type.columns()), 0, type.columns() - 1);
        int rowFromBottom = Mth.clamp(Mth.floor((hit.y - pos.getY()) * type.rows()), 0, type.rows() - 1);
        return (type.rows() - 1 - rowFromBottom) * type.columns() + column;
    }
}
