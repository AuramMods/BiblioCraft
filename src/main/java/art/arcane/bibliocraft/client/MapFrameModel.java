package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.MapFrameBlock;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

final class MapFrameModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Integer> PARTS = new ModelProperty<>();

    MapFrameModel(BakedModel original) {
        super(new SlotVisibilityModel(new FurnitureMaterialModel(original), null, PARTS));
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                           @NotNull BlockState state, @NotNull ModelData data) {
        return super.getModelData(level, pos, state, data).derive()
                .with(PARTS, MapFrameBlock.visibleParts(level, pos, state)).build();
    }
}
