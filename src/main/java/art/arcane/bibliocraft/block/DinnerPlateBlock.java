package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.DinnerPlateBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.DinnerPlateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DinnerPlateBlock extends InventoryFurnitureBlock implements DrillAdjustable {
    public DinnerPlateBlock(Properties properties, VoxelShape shape) {
        super(properties, shape, "dinner_plate");
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableDinnerPlate.get()
                || !(level.getBlockEntity(pos) instanceof DinnerPlateBlockEntity plate)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            interact(level, pos, player, plate);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void interact(Level level, BlockPos pos, Player player, DinnerPlateBlockEntity plate) {
        ItemStack held = player.getMainHandItem();
        if (DinnerPlateBlockEntity.isFood(held)) {
            for (int slot = 0; slot < plate.getContainerSize(); slot++) {
                if (insertStack(plate, slot, held)) {
                    plate.stopEating(player);
                    return;
                }
            }
        }
        if (plate.isEmpty() || player.isShiftKeyDown()) {
            plate.stopEating(player);
            player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new DinnerPlateMenu(id, inventory, plate),
                    getName()));
            return;
        }
        if (!player.getFoodData().needsFood()) {
            plate.stopEating(player);
            return;
        }

        float pitch = 0.9F + level.random.nextFloat() * 0.1F;
        if (!plate.finishBite(player)) {
            level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.5F, pitch);
            return;
        }
        for (int slot = 0; slot < plate.getContainerSize() && player.getFoodData().needsFood(); slot++) {
            if (DinnerPlateBlockEntity.isFood(plate.getItem(slot))) {
                ItemStack serving = plate.removeItem(slot, 1);
                ItemStack remainder = serving.finishUsingItem(level, player);
                if (!remainder.isEmpty() && remainder != serving) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5, remainder);
                }
            }
        }
        level.playSound(player, pos, SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, pitch);
    }
}
