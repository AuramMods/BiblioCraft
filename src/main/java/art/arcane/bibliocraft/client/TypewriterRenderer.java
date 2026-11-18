package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.ColorFacingEntityBlock;
import art.arcane.bibliocraft.block.TypewriterBlock;
import art.arcane.bibliocraft.blockentity.TypewriterBlockEntity;
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
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Arrays;
import java.util.stream.IntStream;

final class TypewriterRenderer implements BlockEntityRenderer<TypewriterBlockEntity> {
    private static final ResourceLocation[] CARRIAGES = Arrays.stream(DyeColor.values())
            .map(color -> part("carriage_" + color.getSerializedName())).toArray(ResourceLocation[]::new);
    private static final ResourceLocation[] PAPER = IntStream.rangeClosed(0, TypewriterBlockEntity.LAST_LINE)
            .mapToObj(progress -> part("paper_" + progress)).toArray(ResourceLocation[]::new);
    private final ModelBlockRenderer modelRenderer;

    TypewriterRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    static void registerModels(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation model : CARRIAGES) {
            event.register(model);
        }
        for (ResourceLocation model : PAPER) {
            event.register(model);
        }
    }

    @Override
    public void render(TypewriterBlockEntity typewriter, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var state = typewriter.getBlockState();
        int progress = typewriter.getProgress();
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(TypewriterBlock.FACING).toYRot()));
        pose.translate(-0.5, 0, -0.5);
        // Advance the original carriage across the local paper feed, independently of the block's facing.
        pose.translate(0, 0, progress == 0 ? 0 : 0.14 - 0.02 * progress);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        renderPart(pose, vertices, CARRIAGES[state.getValue(ColorFacingEntityBlock.COLOR).getId()], light, overlay);
        if (typewriter.hasPaper()) {
            renderPart(pose, vertices, PAPER[progress], light, overlay);
        }
        pose.popPose();
    }

    private void renderPart(PoseStack pose, VertexConsumer vertices, ResourceLocation id, int light, int overlay) {
        var model = Minecraft.getInstance().getModelManager().getModel(id);
        modelRenderer.renderModel(pose.last(), vertices, null, model, 1, 1, 1, light, overlay, ModelData.EMPTY, null);
    }

    private static ResourceLocation part(String name) {
        return ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/typewriter/" + name);
    }
}
