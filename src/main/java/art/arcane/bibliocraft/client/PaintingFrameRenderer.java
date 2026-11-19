package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.PaintingFrameBlock;
import art.arcane.bibliocraft.blockentity.PaintingFrameBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

final class PaintingFrameRenderer implements BlockEntityRenderer<PaintingFrameBlockEntity> {
    PaintingFrameRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PaintingFrameBlockEntity frame, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var art = frame.art();
        var level = frame.getLevel();
        if (art.isBlank() || level == null) {
            return;
        }
        var settings = frame.settings();
        var box = settings.footprint(art, frame.insetArtwork(level) ? 0.1 : 0);
        var texture = ClientPaintingArt.picture(art, settings.aspect(), settings.resolution());
        var vertices = buffers.getBuffer(RenderType.entityCutout(texture));
        pose.pushPose();
        PaintingArtRenderer.faceNorthModel(pose, frame.getBlockState().getValue(PaintingFrameBlock.FACING));
        var matrix = pose.last();
        float left = (float) (1 - box.left());
        float right = (float) (1 - box.right());
        float bottom = (float) box.bottom();
        float top = (float) box.top();
        int rotation = settings.rotation();
        PaintingArtRenderer.vertex(vertices, matrix, .038f, bottom, left, 0, rotation, 1, 0, light, overlay);
        PaintingArtRenderer.vertex(vertices, matrix, .038f, bottom, right, 3, rotation, 1, 0, light, overlay);
        PaintingArtRenderer.vertex(vertices, matrix, .038f, top, right, 2, rotation, 1, 0, light, overlay);
        PaintingArtRenderer.vertex(vertices, matrix, .038f, top, left, 1, rotation, 1, 0, light, overlay);
        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return (int) Math.ceil(BiblioConfig.COMMON.renderDistancePainting.get());
    }

    @Override
    public boolean shouldRenderOffScreen(PaintingFrameBlockEntity frame) {
        return !frame.art().isBlank();
    }

    @Override
    public boolean shouldRender(PaintingFrameBlockEntity frame, Vec3 camera) {
        var box = frame.getRenderBoundingBox();
        double dx = Math.max(0, Math.max(box.minX - camera.x, camera.x - box.maxX));
        double dy = Math.max(0, Math.max(box.minY - camera.y, camera.y - box.maxY));
        double dz = Math.max(0, Math.max(box.minZ - camera.z, camera.z - box.maxZ));
        double distance = BiblioConfig.COMMON.renderDistancePainting.get();
        return dx * dx + dy * dy + dz * dz <= distance * distance;
    }
}
