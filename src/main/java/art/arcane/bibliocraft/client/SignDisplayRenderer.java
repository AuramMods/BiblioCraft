package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.FancySignBlock;
import art.arcane.bibliocraft.block.SignDisplayBlock;
import art.arcane.bibliocraft.blockentity.FancySignBlockEntity;
import art.arcane.bibliocraft.blockentity.FancySignLayout.Icon;
import art.arcane.bibliocraft.blockentity.FancySignLayout.Line;
import art.arcane.bibliocraft.blockentity.FurnitureInventoryBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class SignDisplayRenderer implements BlockEntityRenderer<FurnitureInventoryBlockEntity> {
    static final float PIXEL = 0.004F;
    static final double TEXT_LEFT = 0.047;
    static final double TEXT_TOP = 0.737;
    static final int TEXT_WIDTH = 220;
    static final int TEXT_HEIGHT = 128;
    static final int INK = 0xff19120c;
    private static final float[] ICON_SCALES = {0.08F, 0.16F, 0.24F, 0.31F, 0.39F, 0.47F, 0.55F, 0.64F, 0.71F};
    private static final double[] ICON_X_OFFSETS = {0.020, 0.015, 0.023, 0.029, 0.031, 0.018, 0.022, 0.030, 0.030};
    private static final double[] ICON_Y_OFFSETS = {0.008, 0.010, 0.020, 0.025, 0.030, 0.030, 0.040, 0.050, 0.050};
    private final ItemRenderer items;
    private final Font font;

    SignDisplayRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
        font = context.getFont();
    }

    @Override
    public void render(FurnitureInventoryBlockEntity display, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var state = display.getBlockState();
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(270 - state.getValue(SignDisplayBlock.FACING).toYRot()));
        pose.translate(-0.5, 0, -0.5);
        if (display instanceof FancySignBlockEntity sign) {
            pose.translate(0, 0, FancySignBlock.depth(state));
            renderSign(sign, pose, buffers, light, overlay);
        } else if (display.getItem(1).isEmpty() && display.getItem(2).isEmpty()) {
            renderLabelItem(display, 0, 0.5, 0.25, 0.047, 0.4F, pose, buffers, light, overlay);
        } else {
            renderLabelItem(display, 1, 0.36, 0.29, 0.047, 0.22F, pose, buffers, light, overlay);
            renderLabelItem(display, 0, 0.5, 0.20, 0.051, 0.22F, pose, buffers, light, overlay);
            renderLabelItem(display, 2, 0.64, 0.29, 0.047, 0.22F, pose, buffers, light, overlay);
        }
        pose.popPose();
    }

    private void renderLabelItem(FurnitureInventoryBlockEntity label, int slot, double x, double y, double z, float scale,
                                 PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = label.getItem(slot);
        if (!stack.isEmpty()) {
            pose.pushPose();
            pose.translate(x, y, z);
            pose.scale(scale, scale, scale);
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, label.getLevel(), slot);
            pose.popPose();
        }
    }

    private void renderSign(FancySignBlockEntity sign, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var layout = sign.getLayout();
        for (int slot = 0; slot < sign.getContainerSize(); slot++) {
            ItemStack stack = sign.getItem(slot);
            if (!stack.isEmpty()) {
                Icon icon = layout.icon(slot);
                pose.pushPose();
                pose.translate(iconX(icon), iconY(icon), 0.056 + 0.0015 * slot);
                float scale = ICON_SCALES[icon.size()];
                pose.scale(scale, scale, 0.02F * scale);
                renderIcon(items, stack, icon, sign.getLevel(), slot, pose, buffers, light, overlay);
                pose.popPose();
            }
        }
        int y = 0;
        for (Line line : layout.lines()) {
            if (y + line.lineHeight() > TEXT_HEIGHT) {
                break;
            }
            if (!line.text().isEmpty()) {
                pose.pushPose();
                pose.translate(TEXT_LEFT, TEXT_TOP - y * PIXEL, 0.064);
                float scale = PIXEL * line.scale();
                pose.scale(scale, -scale, scale);
                font.drawInBatch(fitText(font, line), 0, 0, INK, false, pose.last().pose(), buffers,
                        Font.DisplayMode.NORMAL, 0, light);
                pose.popPose();
            }
            y += line.lineHeight();
        }
    }

    static String fitText(Font font, Line line) {
        return font.getSplitter().formattedHeadByWidth(line.text(), (int) (TEXT_WIDTH / line.scale()), Style.EMPTY);
    }

    static double iconX(Icon icon) {
        return 0.503 + icon.x() * PIXEL + ICON_X_OFFSETS[icon.size()];
    }

    static double iconY(Icon icon) {
        return 0.495 - icon.y() * PIXEL - ICON_Y_OFFSETS[icon.size()];
    }

    static float previewIconScale(Icon icon) {
        return ICON_SCALES[icon.size()] / PIXEL;
    }

    static void renderIcon(ItemRenderer items, ItemStack stack, Icon icon, Level level, int slot, PoseStack pose,
                           MultiBufferSource buffers, int light, int overlay) {
        if (items.getModel(stack, level, null, slot).isGui3d()) {
            if (icon.rotation() != 1) {
                pose.mulPose(Axis.XP.rotationDegrees(25));
                pose.mulPose(Axis.YP.rotationDegrees(icon.rotation() == 0 ? 45 : -45));
            }
        } else if (icon.rotation() == 0) {
            pose.mulPose(Axis.YP.rotationDegrees(180));
        }
        items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, level, slot);
    }
}
