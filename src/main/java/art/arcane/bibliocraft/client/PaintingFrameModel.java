package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.PaintingFrameBlock;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

final class PaintingFrameModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Integer> VISIBLE_PARTS = new ModelProperty<>();

    PaintingFrameModel(BakedModel original) {
        super(new SlotVisibilityModel(new FurnitureMaterialModel(original), (state, data) -> {
            Integer parts = data.get(VISIBLE_PARTS);
            return parts == null ? 0 : parts;
        }));
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                           @NotNull BlockState state, @NotNull ModelData data) {
        return super.getModelData(level, pos, state, data).derive()
                .with(VISIBLE_PARTS, PaintingFrameBlock.visibleParts(level, pos, state)).build();
    }
}
