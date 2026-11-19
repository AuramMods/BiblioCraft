package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.ClipboardBlock;
import art.arcane.bibliocraft.blockentity.ClipboardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

final class ClipboardRenderer implements BlockEntityRenderer<ClipboardBlockEntity> {
    private static final float PIXEL = 0.0046F;
    private static final int INK = 0xFF38332B;
    private final Font font;

    ClipboardRenderer(BlockEntityRendererProvider.Context context) {
        font = context.getFont();
    }

    @Override
    public void render(ClipboardBlockEntity board, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        var view = board.display();
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - board.getBlockState().getValue(ClipboardBlock.FACING).toYRot()));
        pose.translate(-0.5, 0, -0.5);
        pose.translate(0.052, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        draw(view.page().title(), -0.226, 0.838, 109, pose, buffers, light);
        for (int index = 0; index < view.page().tasks().size(); index++) {
            draw(view.page().tasks().get(index).text(), -0.204, 0.771 - 0.066 * index, 108, pose, buffers, light);
        }
        String page = Integer.toString(view.current() + 1);
        draw(page, -font.width(page) * PIXEL / 2, 0.169, 18, pose, buffers, light);
        pose.popPose();
    }

    private void draw(String text, double x, double y, int maxWidth, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!text.isEmpty()) {
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(PIXEL, -PIXEL, PIXEL);
            font.drawInBatch(font.plainSubstrByWidth(text, maxWidth), 0, 0, INK, false, pose.last().pose(), buffers,
                    Font.DisplayMode.NORMAL, 0, light);
            pose.popPose();
        }
    }
}
