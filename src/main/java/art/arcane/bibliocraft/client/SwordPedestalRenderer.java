package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.SwordPedestalBlock;
import art.arcane.bibliocraft.blockentity.SwordPedestalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

final class SwordPedestalRenderer implements BlockEntityRenderer<SwordPedestalBlockEntity> {
    private final ItemRenderer items;

    SwordPedestalRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(SwordPedestalBlockEntity pedestal, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        ItemStack sword = pedestal.getItem(0);
        if (sword.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0.65, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(90 - pedestal.getBlockState().getValue(SwordPedestalBlock.FACING).toYRot()));
        pose.mulPose(Axis.ZP.rotationDegrees(135));
        pose.scale(0.9F, 0.9F, 0.9F);
        items.renderStatic(sword, ItemDisplayContext.FIXED, light, overlay, pose, buffers, pedestal.getLevel(), 0);
        pose.popPose();
    }
}
