package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.painting.PaintingArt;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IQuadTransformer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class PaintingCanvasModel extends BakedModelWrapper<BakedModel> {
    private static final ResourceLocation CANVAS = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/canvas");
    private final BakedModel wood;

    PaintingCanvasModel(BakedModel original) {
        super(original);
        wood = new BakedModelWrapper<>(original) {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
                return super.getQuads(state, side, random).stream().filter(quad -> !isCanvas(quad)).toList();
            }
        };
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        BakedModel transformed = super.applyTransform(context, pose, leftHand);
        return transformed == originalModel ? this : new PaintingCanvasModel(transformed);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        var art = PaintingArt.from(stack);
        if (art.isBlank()) {
            return super.getRenderPasses(stack, fabulous);
        }
        var renderType = RenderType.entityCutout(ClientPaintingArt.picture(art, 0, 0));
        return List.of(wood, new BakedModelWrapper<>(originalModel) {
            @Override
            public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
                return List.of(renderType);
            }

            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
                return super.getQuads(state, side, random).stream().filter(PaintingCanvasModel::isCanvas)
                        .map(PaintingCanvasModel::unmapUv).toList();
            }
        });
    }

    private static boolean isCanvas(BakedQuad quad) {
        return quad.getSprite().contents().name().equals(CANVAS);
    }

    private static BakedQuad unmapUv(BakedQuad quad) {
        var sprite = quad.getSprite();
        int[] vertices = quad.getVertices().clone();
        for (int offset = IQuadTransformer.UV0; offset < vertices.length; offset += IQuadTransformer.STRIDE) {
            float u = Float.intBitsToFloat(vertices[offset]);
            float v = Float.intBitsToFloat(vertices[offset + 1]);
            vertices[offset] = Float.floatToRawIntBits(sprite.getUOffset(u) / 16);
            vertices[offset + 1] = Float.floatToRawIntBits(sprite.getVOffset(v) / 16);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), sprite, quad.isShade(),
                quad.hasAmbientOcclusion());
    }
}
