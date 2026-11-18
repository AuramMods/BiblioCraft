package art.arcane.bibliocraft.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.ToIntBiFunction;

final class SlotVisibilityModel extends BakedModelWrapper<BakedModel> {
    private final @Nullable ResourceLocation sprite;
    private final ToIntBiFunction<BlockState, ModelData> visibleParts;

    SlotVisibilityModel(BakedModel original, ResourceLocation sprite, ModelProperty<Integer> visibleParts) {
        this(original, sprite, (state, data) -> {
            Integer parts = data.get(visibleParts);
            return parts == null ? 0 : parts;
        });
    }

    SlotVisibilityModel(BakedModel original, ToIntBiFunction<BlockState, ModelData> visibleParts) {
        this(original, null, visibleParts);
    }

    private SlotVisibilityModel(BakedModel original, @Nullable ResourceLocation sprite,
                                ToIntBiFunction<BlockState, ModelData> visibleParts) {
        super(original);
        this.sprite = sprite;
        this.visibleParts = visibleParts;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                            @NotNull RandomSource random, @NotNull ModelData data,
                                            @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, random, data, renderType);
        if (quads.isEmpty() || state == null) {
            return quads;
        }
        int mask = visibleParts.applyAsInt(state, data);
        // Each authored mesh has a Forge OBJ material whose tint index identifies the part.
        return quads.stream().filter(quad -> quad.getTintIndex() < 0
                || (sprite != null && !sprite.equals(quad.getSprite().contents().name()))
                || (mask & (1 << quad.getTintIndex())) != 0).toList();
    }
}
