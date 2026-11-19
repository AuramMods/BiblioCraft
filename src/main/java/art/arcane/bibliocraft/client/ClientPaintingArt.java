package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.painting.PaintingArt;
import art.arcane.bibliocraft.painting.PaintingArt.Source;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class ClientPaintingArt extends SimplePreparableReloadListener<List<PaintingArt>> {
    private static final String CUSTOM_DIRECTORY = "textures/custompaintings/";
    private static final ResourceLocation MISSING = PaintingArt.texture("paintings/paintingnotfound.png");
    private static final LoadingCache<ResourceLocation, ResourceLocation> TEXTURES = CacheBuilder.newBuilder()
            .maximumSize(512).build(CacheLoader.from(texture ->
                    Minecraft.getInstance().getResourceManager().getResource(texture).isPresent() ? texture : MISSING));
    private static List<PaintingArt> custom = List.of();

    static List<PaintingArt> gallery(Source source) {
        return switch (source) {
            case BIBLIOCRAFT -> PaintingArt.bundled();
            case VANILLA -> BuiltInRegistries.PAINTING_VARIANT.keySet().stream()
                    .sorted().flatMap(id -> PaintingArt.validate(Source.VANILLA, id.toString(), 1, 1).stream())
                    .toList();
            case CUSTOM -> custom;
        };
    }

    static ResourceLocation picture(PaintingArt art, int aspect, int resolution) {
        return TEXTURES.getUnchecked(art.texture(aspect, resolution));
    }

    static void preview(GuiGraphics graphics, PaintingArt art, int aspect, int resolution, int rotation,
                        int x, int y, int maxWidth, int maxHeight) {
        var picture = art.image(aspect);
        boolean turn = (rotation & 1) != 0;
        int availableWidth = turn ? maxHeight : maxWidth;
        int availableHeight = turn ? maxWidth : maxHeight;
        float scale = Math.min((float) availableWidth / picture.width(), (float) availableHeight / picture.height());
        int width = Math.max(1, Math.round(picture.width() * scale));
        int height = Math.max(1, Math.round(picture.height() * scale));
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + maxWidth / 2.0, y + maxHeight / 2.0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(rotation * 90));
        graphics.blit(picture(art, aspect, resolution), -width / 2, -height / 2,
                width, height, 0, 0, 1, 1, 1, 1);
        pose.popPose();
    }

    @Override
    protected List<PaintingArt> prepare(ResourceManager manager, ProfilerFiller profiler) {
        return manager.listResources("textures/custompaintings",
                id -> id.getNamespace().equals(BiblioCraft.MODID) && id.getPath().endsWith(".png"))
                .entrySet().stream()
                .flatMap(entry -> readPng(entry.getKey(), entry.getValue()).stream())
                .sorted(Comparator.comparing(PaintingArt::title)).toList();
    }

    private static Optional<PaintingArt> readPng(ResourceLocation id, Resource resource) {
        try (var input = new DataInputStream(resource.open())) {
            if (input.readLong() != 0x89504e470d0a1a0aL || input.readInt() != 13 || input.readInt() != 0x49484452) {
                return Optional.empty();
            }
            int width = input.readInt();
            int height = input.readInt();
            if (width <= 0 || height <= 0) {
                return Optional.empty();
            }
            int shortSide = Math.min(width, height);
            int across = Math.min(32, Math.round((float) width / shortSide));
            int tall = Math.min(32, Math.round((float) height / shortSide));
            return PaintingArt.validate(Source.CUSTOM, id.getPath().substring(CUSTOM_DIRECTORY.length()), across, tall);
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    @Override
    protected void apply(List<PaintingArt> artworks, ResourceManager manager, ProfilerFiller profiler) {
        custom = artworks;
        TEXTURES.invalidateAll();
    }
}
