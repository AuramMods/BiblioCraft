package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.DeskBlock;
import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;

final class DeskRenderer implements BlockEntityRenderer<DeskBlockEntity> {
    private static final RenderType MAP_BACKGROUND = RenderType.text(
            ResourceLocation.withDefaultNamespace("textures/map/map_background.png"));

    DeskRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(DeskBlockEntity desk, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        var stack = desk.getItem(DeskBlockEntity.OPEN_BOOK);
        if (!stack.is(Items.FILLED_MAP) || desk.getLevel() == null) {
            return;
        }
        Integer id = MapItem.getMapId(stack);
        var map = MapItem.getSavedData(id, desk.getLevel());
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - desk.getBlockState().getValue(DeskBlock.FACING).toYRot()));
        pose.translate(0.23, 1.004, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.mulPose(Axis.XP.rotationDegrees(90));
        float scale = 0.45F / 128;
        pose.scale(scale, scale, scale);
        pose.translate(-64, -64, 0);
        var matrix = pose.last().pose();
        var vertices = buffers.getBuffer(MAP_BACKGROUND);
        vertices.vertex(matrix, -7, 135, 0).color(255, 255, 255, 255).uv(0, 1).uv2(light).endVertex();
        vertices.vertex(matrix, 135, 135, 0).color(255, 255, 255, 255).uv(1, 1).uv2(light).endVertex();
        vertices.vertex(matrix, 135, -7, 0).color(255, 255, 255, 255).uv(1, 0).uv2(light).endVertex();
        vertices.vertex(matrix, -7, -7, 0).color(255, 255, 255, 255).uv(0, 0).uv2(light).endVertex();
        if (id != null && map != null) {
            Minecraft.getInstance().gameRenderer.getMapRenderer().render(pose, buffers, id, map, false, light);
        }
        pose.popPose();
    }
}
