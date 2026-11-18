package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.DinnerPlateBlock;
import art.arcane.bibliocraft.blockentity.DinnerPlateBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

final class DinnerPlateRenderer implements BlockEntityRenderer<DinnerPlateBlockEntity> {
    private final ItemRenderer items;

    DinnerPlateRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(DinnerPlateBlockEntity plate, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - plate.getBlockState().getValue(DinnerPlateBlock.FACING).toYRot()));
        pose.translate(-0.5, 0, -0.5);

        if (plate.getItem(1).isEmpty() && plate.getItem(2).isEmpty()) {
            renderPile(plate, 0, 0.5, 0.05, 0.5, 0.7F, 0.05, pose, buffers, light, overlay);
        } else {
            renderPile(plate, 0, 0.5, 0.07, 0.68, 0.5F, 0.035, pose, buffers, light, overlay);
            renderPile(plate, 1, 0.35, 0.068, 0.4, 0.4F, 0.03, pose, buffers, light, overlay);
            renderPile(plate, 2, 0.62, 0.066, 0.4, 0.4F, 0.03, pose, buffers, light, overlay);
        }
        pose.popPose();
    }

    private void renderPile(DinnerPlateBlockEntity plate, int slot, double x, double y, double z, float scale,
                            double step, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = plate.getItem(slot);
        if (stack.isEmpty()) {
            return;
        }
        int layers = stack.getCount() == 1 ? 1 : stack.getCount() / 20 + 2;
        for (int layer = 0; layer < layers; layer++) {
            pose.pushPose();
            pose.translate(x, y + step * layer, z);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.scale(scale, scale, scale);
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, plate.getLevel(), slot);
            pose.popPose();
        }
    }
}
