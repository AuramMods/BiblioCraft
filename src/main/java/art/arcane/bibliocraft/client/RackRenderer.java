package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.RackBlock;
import art.arcane.bibliocraft.block.RackType;
import art.arcane.bibliocraft.blockentity.RackBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class RackRenderer implements BlockEntityRenderer<RackBlockEntity> {
    private static final double[] POTION_HEIGHTS = {0.86, 0.53, 0.18};
    private final ItemRenderer items;

    RackRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(RackBlockEntity rack, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        RackType type = rack.type();
        boolean potions = type == RackType.POTIONS;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - rack.getBlockState().getValue(RackBlock.FACING).toYRot()));
        pose.translate(-0.5, 0, -0.5);
        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            ItemStack stack = rack.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Vec3 center = itemCenter(type, slot);
            pose.pushPose();
            pose.translate(center.x, center.y, center.z);
            pose.mulPose(Axis.YP.rotationDegrees(potions ? -90 : 90));
            float scale = potions ? 0.42F : 0.6F;
            pose.scale(scale, scale, scale);
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, rack.getLevel(), slot);
            pose.popPose();
        }
        pose.popPose();
    }

    private static Vec3 itemCenter(RackType type, int slot) {
        int row = slot / type.columns();
        int column = slot % type.columns();
        // The potion OBJ sits in the east half and the tool rack is mounted on the west half.
        if (type == RackType.POTIONS) {
            return new Vec3(0.75, POTION_HEIGHTS[row], 0.15 + column * 0.23);
        }
        return new Vec3(row == 0 ? 0.43 : 0.45, row == 0 ? 0.67 : 0.31, 0.68 - column * 0.36);
    }
}
