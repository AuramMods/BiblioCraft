package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.DiscRackBlock;
import art.arcane.bibliocraft.block.DiscRackMount;
import art.arcane.bibliocraft.blockentity.DiscRackBlockEntity;
import art.arcane.bibliocraft.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

final class DiscRackRenderer implements BlockEntityRenderer<DiscRackBlockEntity> {
    private final ItemRenderer items;

    DiscRackRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(DiscRackBlockEntity rack, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = rack.getBlockState();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(DiscRackBlock.FACING).toYRot()));
        applyMount(pose, state.getValue(DiscRackBlock.MOUNT));
        pose.translate(-0.5, -0.5, -0.5);

        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            ItemStack stack = rack.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(DiscRackBlock.FIRST_SLOT_CENTER - DiscRackBlock.SLOT_SPACING * slot, 0.46, 0.5);
            ItemDisplayContext context = ItemDisplayContext.FIXED;
            if (stack.is(ModBlocks.DINNER_PLATE.get().asItem())) {
                // Stand the plate on its edge and center its thin geometry in the groove.
                pose.mulPose(Axis.ZP.rotationDegrees(-90));
                pose.scale(0.86F, 0.86F, 0.86F);
                pose.translate(0, 0.46, 0);
                context = ItemDisplayContext.NONE;
            } else {
                pose.mulPose(Axis.YP.rotationDegrees(90));
                boolean threeDimensional = items.getModel(stack, rack.getLevel(), null, slot).isGui3d();
                pose.scale(0.86F, 0.86F, threeDimensional ? 0.15F : 0.86F);
            }
            items.renderStatic(stack, context, light, overlay, pose, buffers, rack.getLevel(), slot);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void applyMount(PoseStack pose, DiscRackMount mount) {
        // These rotations match the root transforms on the corresponding OBJ models.
        switch (mount) {
            case FLOOR -> {}
            case WALL -> pose.mulPose(Axis.ZP.rotationDegrees(-90));
            case WALL_HORIZONTAL -> {
                pose.mulPose(Axis.XP.rotationDegrees(-90));
                pose.mulPose(Axis.ZP.rotationDegrees(-90));
            }
            case CEILING -> pose.mulPose(Axis.ZP.rotationDegrees(180));
        }
    }
}
