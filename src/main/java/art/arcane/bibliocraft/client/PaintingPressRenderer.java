package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.PaintingPressBlock;
import art.arcane.bibliocraft.blockentity.PaintingPressBlockEntity;
import art.arcane.bibliocraft.painting.PaintingArt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

final class PaintingPressRenderer implements BlockEntityRenderer<PaintingPressBlockEntity> {
    static final ResourceLocation LID = ResourceLocation.fromNamespaceAndPath(
            BiblioCraft.MODID, "block/painting_press_lid");
    private final ModelBlockRenderer renderer;

    PaintingPressRenderer(BlockEntityRendererProvider.Context context) {
        renderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    @Override
    public void render(PaintingPressBlockEntity press, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        pose.pushPose();
        PaintingArtRenderer.faceNorthModel(pose, press.getBlockState().getValue(PaintingPressBlock.FACING));
        pose.pushPose();
        pose.translate(0, 0.42, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(press.lidAngle(partialTick)));
        var model = Minecraft.getInstance().getModelManager().getModel(LID);
        renderer.renderModel(pose.last(), buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)),
                null, model, 1, 1, 1, light, overlay, ModelData.EMPTY, null);
        pose.popPose();
        var art = PaintingArt.from(press.getItem(PaintingPressBlockEntity.CANVAS));
        if (!art.isBlank()) {
            var vertices = buffers.getBuffer(RenderType.entityCutout(ClientPaintingArt.picture(art, 0, 0)));
            var matrix = pose.last();
            PaintingArtRenderer.vertex(vertices, matrix, .9f, .375f, .9f, 0, 0, 0, 1, light, overlay);
            PaintingArtRenderer.vertex(vertices, matrix, .9f, .375f, .1f, 3, 0, 0, 1, light, overlay);
            PaintingArtRenderer.vertex(vertices, matrix, .1f, .375f, .1f, 2, 0, 0, 1, light, overlay);
            PaintingArtRenderer.vertex(vertices, matrix, .1f, .375f, .9f, 1, 0, 0, 1, light, overlay);
        }
        pose.popPose();
    }
}
