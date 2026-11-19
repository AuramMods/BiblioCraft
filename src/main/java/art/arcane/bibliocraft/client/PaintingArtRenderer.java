package art.arcane.bibliocraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;

final class PaintingArtRenderer {
    private PaintingArtRenderer() {}

    static void faceNorthModel(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        pose.translate(-0.5, 0, -0.5);
    }

    static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z, int corner,
                       int rotation, float nx, float ny, int light, int overlay) {
        int uv = (corner + 4 - rotation) & 3;
        vertices.vertex(pose.pose(), x, y, z).color(255, 255, 255, 255)
                .uv(uv == 0 || uv == 1 ? 0 : 1, uv == 0 || uv == 3 ? 1 : 0)
                .overlayCoords(overlay).uv2(light).normal(pose.normal(), nx, ny, 0).endVertex();
    }
}
