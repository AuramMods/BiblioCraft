package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.block.DrillAdjustable;
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
                || !(state.getBlock() instanceof DrillAdjustable adjustable)
                || !BiblioFeatureToggles.isBlockEnabled(ForgeRegistries.BLOCKS.getKey(state.getBlock()))) {
            return InteractionResult.PASS;
        }
        var adjusted = adjustable.adjustWithDrill(state, player.isShiftKeyDown());
        if (adjusted == state) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, adjusted, Block.UPDATE_ALL);
            level.playSound(null, pos, (manual ? ModSounds.DRILL : ModSounds.SCREW).get(),
                    SoundSource.BLOCKS, manual ? 0.8F : 0.7F, 1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
