package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.BookcaseBlock;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.blockentity.BookcaseBlockEntity;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.blockentity.FurniturePanelerBlockEntity;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class FurnitureMaterialModel extends BakedModelWrapper<BakedModel> {
    private static final ResourceLocation WOOD_SPRITE = FurnitureMaterial.Wood.OAK.texture();
    private static final ResourceLocation PANEL_SPRITE =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/typewriter_paper_blank");
    private static final ResourceLocation BOOKCASE_BOOKS_SPRITE =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/bookcase_books");
    private final LoadingCache<QuadTexture, BakedQuad> quads = CacheBuilder.newBuilder()
            .maximumSize(4096)
            .build(CacheLoader.from(FurnitureMaterialModel::replaceSprite));
    private final LoadingCache<ItemPass, BakedModel> itemPasses = CacheBuilder.newBuilder()
            .maximumSize(128)
            .build(CacheLoader.from(this::itemPass));

    FurnitureMaterialModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource random, @NotNull ModelData data,
                                             @Nullable RenderType renderType) {
        List<BakedQuad> original = super.getQuads(state, side, random, data, renderType);
        if (original.isEmpty()) {
            return original;
        }
        if (state != null && state.getBlock() instanceof BookcaseBlock) {
            Integer occupied = data.get(BookcaseBlockEntity.OCCUPIED_SLOTS);
            int slots = occupied == null ? 0 : occupied;
            // Each book material carries its authored slot as the Forge OBJ tint index.
            original = original.stream().filter(quad -> !BOOKCASE_BOOKS_SPRITE.equals(quad.getSprite().contents().name())
                    || (slots & (1 << quad.getTintIndex())) != 0).toList();
        }
        FurnitureMaterial material = data.get(FurnitureBlockEntity.MATERIAL);
        TextureAtlasSprite wood = isDefault(material) ? null : sprite(material);
        TextureAtlasSprite panel = panelSprite(data.get(FurniturePanelerBlockEntity.PANEL_SOURCE));
        return wood == null && panel == null ? original : retexture(original, wood, panel);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        FurnitureMaterial material = data.get(FurnitureBlockEntity.MATERIAL);
        return isDefault(material) ? super.getParticleIcon(data) : sprite(material);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        BakedModel transformed = super.applyTransform(context, pose, leftHand);
        return transformed == originalModel ? this : new FurnitureMaterialModel(transformed);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        List<BakedModel> passes = super.getRenderPasses(stack, fabulous);
        FurnitureMaterial material = FurnitureMaterial.fromItem(stack);
        if (isDefault(material)) {
            return passes;
        }
        TextureAtlasSprite sprite = sprite(material);
        return passes.stream().map(pass -> itemPasses.getUnchecked(new ItemPass(pass, sprite))).toList();
    }

    private BakedModel itemPass(ItemPass pass) {
        return new BakedModelWrapper<>(pass.model()) {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
                return retexture(super.getQuads(state, side, random), pass.sprite(), null);
            }
        };
    }

    private List<BakedQuad> retexture(List<BakedQuad> original, @Nullable TextureAtlasSprite wood,
                                       @Nullable TextureAtlasSprite panel) {
        if (original.isEmpty()) {
            return original;
        }
        return original.stream()
                .map(quad -> {
                    ResourceLocation source = quad.getSprite().contents().name();
                    TextureAtlasSprite texture = source.equals(WOOD_SPRITE) ? wood
                            : source.equals(PANEL_SPRITE) ? panel : null;
                    return texture == null ? quad : quads.getUnchecked(new QuadTexture(quad, texture));
                })
                .toList();
    }

    private static boolean isDefault(@Nullable FurnitureMaterial material) {
        return material == null || FurnitureMaterial.OAK.equals(material);
    }

    private static TextureAtlasSprite sprite(FurnitureMaterial material) {
        TextureAtlasSprite panel = panelSprite(material.panel());
        return panel == null ? Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(material.wood().texture()) : panel;
    }

    @Nullable
    @SuppressWarnings("deprecation")
    private static TextureAtlasSprite panelSprite(@Nullable ResourceLocation source) {
        if (source == null) {
            return null;
        }
        var block = BuiltInRegistries.BLOCK.getOptional(source);
        if (block.isEmpty()) {
            return null;
        }
        TextureAtlasSprite panel = Minecraft.getInstance().getBlockRenderer().getBlockModel(block.get().defaultBlockState())
                .getParticleIcon(ModelData.EMPTY);
        return panel.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)
                && !MissingTextureAtlasSprite.getLocation().equals(panel.contents().name()) ? panel : null;
    }

    private static BakedQuad replaceSprite(QuadTexture entry) {
        BakedQuad quad = entry.quad();
        TextureAtlasSprite old = quad.getSprite();
        TextureAtlasSprite sprite = entry.sprite();
        int[] vertices = quad.getVertices().clone();
        for (int offset = IQuadTransformer.UV0; offset < vertices.length; offset += IQuadTransformer.STRIDE) {
            float u = Float.intBitsToFloat(vertices[offset]);
            float v = Float.intBitsToFloat(vertices[offset + 1]);
            vertices[offset] = Float.floatToRawIntBits(sprite.getU(old.getUOffset(u)));
            vertices[offset + 1] = Float.floatToRawIntBits(sprite.getV(old.getVOffset(v)));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), sprite, quad.isShade(),
                quad.hasAmbientOcclusion());
    }

    private record QuadTexture(BakedQuad quad, TextureAtlasSprite sprite) {}
    private record ItemPass(BakedModel model, TextureAtlasSprite sprite) {}
}
