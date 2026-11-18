package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.DisplayCaseBlock;
import art.arcane.bibliocraft.blockentity.DisplayCaseBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

final class DisplayCaseRenderer implements BlockEntityRenderer<DisplayCaseBlockEntity> {
    private final ItemRenderer items;

    DisplayCaseRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(DisplayCaseBlockEntity displayCase, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = displayCase.getItem(DisplayCaseBlockEntity.DISPLAY_SLOT);
        if (stack.isEmpty()) {
            return;
        }

        BlockState state = displayCase.getBlockState();
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(DisplayCaseBlock.FACING).toYRot()));
        pose.translate(-0.5, 0, -0.5);
        // The floor transform maps wall coordinates (x, y, z) to (1 - z, x, 1 - y).
        if (state.getValue(DisplayCaseBlock.FACE) == AttachFace.FLOOR) {
            pose.translate(0.5, 0.36, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
        } else {
            pose.translate(0.36, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(90));
        }
        float scale = items.getModel(stack, displayCase.getLevel(), null, 0).isGui3d() ? 0.3F : 0.8F;
        pose.scale(scale, scale, scale);
        items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, displayCase.getLevel(), 0);
        pose.popPose();
    }
}
