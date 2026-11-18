package art.arcane.bibliocraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;

final class DisplayedMapRenderer {
    private static final RenderType BACKGROUND = RenderType.text(
            ResourceLocation.withDefaultNamespace("textures/map/map_background.png"));

    private DisplayedMapRenderer() {}

    static void render(PoseStack pose, MultiBufferSource buffers, ItemStack stack, Level level, float width, int light) {
        Integer id = MapItem.getMapId(stack);
        var map = MapItem.getSavedData(id, level);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        float scale = width / 128;
        pose.scale(scale, scale, scale);
        pose.translate(-64, -64, 0);
        var matrix = pose.last().pose();
        var vertices = buffers.getBuffer(BACKGROUND);
        vertices.vertex(matrix, -7, 135, 0).color(255, 255, 255, 255).uv(0, 1).uv2(light).endVertex();
        vertices.vertex(matrix, 135, 135, 0).color(255, 255, 255, 255).uv(1, 1).uv2(light).endVertex();
        vertices.vertex(matrix, 135, -7, 0).color(255, 255, 255, 255).uv(1, 0).uv2(light).endVertex();
        vertices.vertex(matrix, -7, -7, 0).color(255, 255, 255, 255).uv(0, 0).uv2(light).endVertex();
        if (id != null && map != null) {
            Minecraft.getInstance().gameRenderer.getMapRenderer().render(pose, buffers, id, map, false, light);
        }
    }
}
