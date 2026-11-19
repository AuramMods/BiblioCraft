package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.ShelfBlock;
import art.arcane.bibliocraft.blockentity.ShelfBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ShelfRenderer implements BlockEntityRenderer<ShelfBlockEntity> {
    private final ItemRenderer items;

    public ShelfRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(ShelfBlockEntity shelf, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        var state = shelf.getBlockState();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(ShelfBlock.FACING).toYRot()));
        pose.translate(((ShelfBlock) state.getBlock()).localShift(state) - 0.5, 0, -0.5);
        for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
            ItemStack stack = shelf.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.25, slot < 2 ? 0.76 : 0.27, slot % 2 == 0 ? 0.75 : 0.25);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            float scale = stack.getItem() instanceof BlockItem ? 0.6F : 0.42F;
            pose.scale(scale, scale, scale);
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, shelf.getLevel(), slot);
            pose.popPose();
        }
        pose.popPose();
    }
}
