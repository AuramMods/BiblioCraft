package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FramedChestBlock;
import art.arcane.bibliocraft.block.FramedChestPart;
import art.arcane.bibliocraft.blockentity.FramedChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

final class FramedChestRenderer implements BlockEntityRenderer<FramedChestBlockEntity> {
    private static final List<ResourceLocation> LIDS = List.of(part("lid"), part("lid_left"), part("lid_right"));
    private static final ResourceLocation LATCH = part("latch");
    private final ModelBlockRenderer modelRenderer;
    private final ItemRenderer itemRenderer;

    FramedChestRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        itemRenderer = context.getItemRenderer();
    }

    static void registerModels(ModelEvent.RegisterAdditional event) {
        LIDS.forEach(event::register);
        event.register(LATCH);
    }

    static boolean isLid(ResourceLocation id) {
        return LIDS.contains(id);
    }

    @Override
    public void render(FramedChestBlockEntity chest, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var state = chest.getBlockState();
        FramedChestPart part = state.getValue(FramedChestBlock.PART);
        float closed = 1 - chest.openness(partialTick);
        float angle = (1 - closed * closed * closed) * 90;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(FramedChestBlock.FACING).toYRot()));
        pose.translate(-0.5, 0, -0.5);

        pose.pushPose();
        pose.translate(0.05, 0.625, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        renderPart(pose, vertices, LIDS.get(part.ordinal()), chest.getModelData(), light, overlay);
        if (part != FramedChestPart.RIGHT) {
            if (part == FramedChestPart.LEFT) {
                pose.translate(0, 0, -0.5);
            }
            renderPart(pose, vertices, LATCH, ModelData.EMPTY, light, overlay);
        }
        pose.popPose();

        if (!chest.getLabel().isEmpty()) {
            pose.translate(0.975, 0.28, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale(0.5F, 0.5F, 0.5F);
            itemRenderer.renderStatic(chest.getLabel(), ItemDisplayContext.FIXED, light, overlay, pose, buffers,
                    chest.getLevel(), (int) chest.getBlockPos().asLong());
        }
        pose.popPose();
    }

    private void renderPart(PoseStack pose, VertexConsumer vertices, ResourceLocation id, ModelData data, int light, int overlay) {
        var model = Minecraft.getInstance().getModelManager().getModel(id);
        modelRenderer.renderModel(pose.last(), vertices, null, model, 1, 1, 1, light, overlay, data, null);
    }

    private static ResourceLocation part(String name) {
        return ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/framed_chest/" + name);
    }
}
