package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.TableBlock;
import art.arcane.bibliocraft.blockentity.TableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;

final class TableRenderer implements BlockEntityRenderer<TableBlockEntity> {
    private final ItemRenderer items;

    TableRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(TableBlockEntity table, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        var stack = table.getItem(TableBlockEntity.DISPLAY);
        var level = table.getLevel();
        if (stack.isEmpty() || level == null) {
            return;
        }
        pose.pushPose();
        if (stack.is(Items.FILLED_MAP)) {
            pose.translate(0.5, 1.02, 0.5);
            DisplayedMapRenderer.render(pose, buffers, stack, level, 0.8064F, light);
        } else {
            pose.translate(0.5, 1.19, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(270 - table.getBlockState().getValue(TableBlock.FACING).toYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(90 * table.xAngle()));
            pose.mulPose(Axis.YP.rotationDegrees(90 * table.yAngle()));
            pose.scale(0.75F, 0.75F, 0.75F);
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, level, 0);
        }
        pose.popPose();
    }
}
