package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.block.ArmorStandBlock;
import art.arcane.bibliocraft.block.ClockBlock;
import art.arcane.bibliocraft.block.DeskBlock;
import art.arcane.bibliocraft.block.DrillAdjustable;
import art.arcane.bibliocraft.block.SeatBlock;
import art.arcane.bibliocraft.block.TableBlock;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public final class FurnitureDrillItem extends Item {
    private final boolean manual;

    public FurnitureDrillItem(Properties properties, boolean manual) {
        super(properties);
        this.manual = manual;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var player = context.getPlayer();
        if (player == null || !BiblioFeatureToggles.isItemEnabled(ForgeRegistries.ITEMS.getKey(this))
                || !BiblioFeatureToggles.isBlockEnabled(ForgeRegistries.BLOCKS.getKey(state.getBlock()))) {
            return InteractionResult.PASS;
        }
        var block = state.getBlock();
        boolean sneaking = player.isShiftKeyDown();
        boolean paired = sneaking && (block instanceof DeskBlock || block instanceof ArmorStandBlock);
        if (!(block instanceof DrillAdjustable || block instanceof TableBlock || block instanceof SeatBlock
                || block instanceof ClockBlock) && !paired) {
            return InteractionResult.PASS;
        }
        var furniture = FurnitureBlockEntity.findAt(level, pos);
        if (furniture != null && !furniture.canAccess(player)) {
            return FurnitureBlockEntity.denyAccess(player);
        }
        if (block instanceof DeskBlock desk) {
            var result = desk.connectWithDrill(context);
            if (!level.isClientSide && result.consumesAction()) {
                playSound(context);
            }
            return result;
        }
        if (block instanceof ArmorStandBlock stand) {
            if (!level.isClientSide) {
                stand.rotateStand(level, pos, state);
                playSound(context);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (block instanceof TableBlock table) {
            var result = table.adjustWithDrill(context, sneaking);
            if (!level.isClientSide && result.consumesAction()) {
                playSound(context);
            }
            return result;
        }
        if (block instanceof SeatBlock seat) {
            var result = seat.adjustWithDrill(context, sneaking);
            if (!level.isClientSide && result.consumesAction()) {
                playSound(context);
            }
            return result;
        }
        if (block instanceof ClockBlock clock) {
            var result = clock.adjustWithDrill(context, sneaking);
            if (!level.isClientSide && result.consumesAction()) {
                playSound(context);
            }
            return result;
        }
        var adjusted = ((DrillAdjustable) block).adjustWithDrill(state, sneaking);
        if (adjusted == state) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, adjusted, Block.UPDATE_ALL);
            playSound(context);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void playSound(UseOnContext context) {
        context.getLevel().playSound(null, context.getClickedPos(), (manual ? ModSounds.DRILL : ModSounds.SCREW).get(),
                SoundSource.BLOCKS, manual ? 0.8F : 0.7F, 1);
    }
}
