package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.PrintingPressBlockEntity;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

final class PrintingPressModel extends BakedModelWrapper<BakedModel> {
    private static final ResourceLocation[] INK = IntStream.rangeClosed(0, 8).mapToObj(level ->
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/inkplate" + level))
            .toArray(ResourceLocation[]::new);
    private final LoadingCache<FurnitureMaterialModel.QuadTexture, BakedQuad> inkQuads = CacheBuilder.newBuilder()
            .maximumSize(64).build(CacheLoader.from(FurnitureMaterialModel::replaceSprite));

    PrintingPressModel(BakedModel original) {
        super(new SlotVisibilityModel(original, (state, data) -> {
            Integer parts = data.get(PrintingPressBlockEntity.VISIBLE_PARTS);
            return parts == null ? 0 : parts;
        }));
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                            @NotNull RandomSource random, @NotNull ModelData data,
                                            @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, random, data, renderType);
        Integer level = data.get(PrintingPressBlockEntity.INK_LEVEL);
        if (quads.isEmpty() || level == null || level == 0) {
            return quads;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(INK[level]);
        return quads.stream().map(quad -> quad.getSprite().contents().name().equals(INK[0])
                ? inkQuads.getUnchecked(new FurnitureMaterialModel.QuadTexture(quad, sprite)) : quad).toList();
    }
}
