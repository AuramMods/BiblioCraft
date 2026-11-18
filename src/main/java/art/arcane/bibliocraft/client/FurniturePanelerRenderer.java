package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.FurniturePanelerBlock;
import art.arcane.bibliocraft.blockentity.FurniturePanelerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FurniturePanelerRenderer implements BlockEntityRenderer<FurniturePanelerBlockEntity> {
    private final ItemRenderer items;

    public FurniturePanelerRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(FurniturePanelerBlockEntity paneler, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        ItemStack input = paneler.getItem(FurniturePanelerBlockEntity.FURNITURE);
        if (input.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - paneler.getBlockState().getValue(FurniturePanelerBlock.FACING).toYRot()));
        pose.translate(-0.5, 0, -0.5);
        renderItem(paneler, input, pose, buffers, light, overlay, 0.29, 1);
        renderItem(paneler, paneler.getResult(), pose, buffers, light, overlay, 0.72, 2);
        pose.popPose();
    }

    private void renderItem(FurniturePanelerBlockEntity paneler, ItemStack stack, PoseStack pose,
                            MultiBufferSource buffers, int light, int overlay, double z, int seed) {
        if (!stack.isEmpty()) {
            pose.pushPose();
            pose.translate(0.24, 0.74, z);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale(0.45F, 0.45F, 0.45F);
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, paneler.getLevel(), seed);
            pose.popPose();
        }
    }
}
