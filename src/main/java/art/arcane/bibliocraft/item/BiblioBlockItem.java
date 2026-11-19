package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public class BiblioBlockItem extends BlockItem {
    public BiblioBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return BiblioFeatureToggles.isBlockEnabled(BuiltInRegistries.BLOCK.getKey(getBlock()))
                ? super.place(context) : InteractionResult.FAIL;
    }
}
