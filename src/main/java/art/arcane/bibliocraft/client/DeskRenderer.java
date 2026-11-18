package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.DeskBlock;
import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.Items;

final class DeskRenderer implements BlockEntityRenderer<DeskBlockEntity> {
    DeskRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(DeskBlockEntity desk, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        var stack = desk.getItem(DeskBlockEntity.OPEN_BOOK);
        if (!stack.is(Items.FILLED_MAP) || desk.getLevel() == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - desk.getBlockState().getValue(DeskBlock.FACING).toYRot()));
        pose.translate(0.23, 1.004, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        DisplayedMapRenderer.render(pose, buffers, stack, desk.getLevel(), 0.45F, light);
        pose.popPose();
    }
}
