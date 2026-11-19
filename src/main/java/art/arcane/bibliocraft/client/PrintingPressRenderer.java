package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.PrintingPressBlock;
import art.arcane.bibliocraft.blockentity.PrintingPressBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;

final class PrintingPressRenderer implements BlockEntityRenderer<PrintingPressBlockEntity> {
    private static final ResourceLocation ARM = ResourceLocation.fromNamespaceAndPath(
            BiblioCraft.MODID, "block/printing_press/arm");
    private static final ResourceLocation BED = ResourceLocation.fromNamespaceAndPath(
            BiblioCraft.MODID, "block/printing_press/bed");
    private final ModelBlockRenderer renderer;

    PrintingPressRenderer(BlockEntityRendererProvider.Context context) {
        renderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(ARM);
        event.register(BED);
    }

    @Override
    public void render(PrintingPressBlockEntity press, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var facing = press.getBlockState().getValue(PrintingPressBlock.FACING);
        float elapsed = press.animationTime(partialTick);
        float stroke = 1 - Math.abs(elapsed % 22 - 11) / 11;
        float armAngle = -5 - 110 * stroke;
        float bedAngle = elapsed <= 11 ? 0 : 25 * (1 - stroke);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        pose.translate(-0.5, 0, -0.5);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        // The original OBJ exports the moving meshes relative to each pivot.
        renderPart(pose, vertices, ARM, 0.12, 0.91, armAngle, light, overlay);
        renderPart(pose, vertices, BED, 0.30, 0.55, bedAngle, light, overlay);
        pose.popPose();
    }

    private void renderPart(PoseStack pose, VertexConsumer vertices, ResourceLocation id, double x, double y,
                            float angle, int light, int overlay) {
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        var model = Minecraft.getInstance().getModelManager().getModel(id);
        renderer.renderModel(pose.last(), vertices, null, model, 1, 1, 1, light, overlay, ModelData.EMPTY, null);
        pose.popPose();
    }
}
