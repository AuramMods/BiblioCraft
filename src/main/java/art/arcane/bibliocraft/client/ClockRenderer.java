package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.ClockBlock;
import art.arcane.bibliocraft.block.ClockPart;
import art.arcane.bibliocraft.blockentity.ClockBlockEntity;
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

import java.util.List;

final class ClockRenderer implements BlockEntityRenderer<ClockBlockEntity> {
    static final ResourceLocation GRANDFATHER_CASE = part("grandfather_case");
    private static final ResourceLocation HOUR_HAND = part("hour_hand");
    private static final ResourceLocation MINUTE_HAND = part("minute_hand");
    private static final ResourceLocation SMALL_PENDULUM = part("small_pendulum");
    private static final ResourceLocation LARGE_PENDULUM = part("large_pendulum");
    private final ModelBlockRenderer modelRenderer;

    ClockRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    static void registerModels(ModelEvent.RegisterAdditional event) {
        List.of(GRANDFATHER_CASE, HOUR_HAND, MINUTE_HAND, SMALL_PENDULUM, LARGE_PENDULUM).forEach(event::register);
    }

    @Override
    public void render(ClockBlockEntity clock, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var state = clock.getBlockState();
        ClockPart part = state.getValue(ClockBlock.PART);
        if (part == ClockPart.LOWER || clock.getLevel() == null) {
            return;
        }
        boolean tall = part == ClockPart.UPPER;
        long time = clock.getLevel().getDayTime();
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(ClockBlock.FACING).toYRot()));
        pose.translate(ClockBlock.depth(state) - 0.5, 0, -0.5);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        if (tall) {
            pose.pushPose();
            pose.translate(0.17, 0, 0);
            renderPart(pose, vertices, GRANDFATHER_CASE, clock.getModelData(), light, overlay);
            pose.popPose();
        }
        int ticksInDay = Math.floorMod(time, 24000);
        float hour = (ticksInDay / 1000) * 15;
        float minute = (ticksInDay % 1000 * 60 / 1000) * 6;
        float swing = (float) ((tall ? 5 : 20) * Math.cos(Math.floorMod(time, 40) * (2 * Math.PI / 40)));
        renderMovingPart(pose, vertices, HOUR_HAND, 0.237, 0.715, hour, light, overlay);
        renderMovingPart(pose, vertices, MINUTE_HAND, 0.252, 0.715, minute, light, overlay);
        renderMovingPart(pose, vertices, tall ? LARGE_PENDULUM : SMALL_PENDULUM, 0.21, 0.5, swing, light, overlay);
        pose.popPose();
    }

    private void renderMovingPart(PoseStack pose, VertexConsumer vertices, ResourceLocation id, double depth,
                                  double height, float angle, int light, int overlay) {
        pose.pushPose();
        pose.translate(depth, height, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.mulPose(Axis.XP.rotationDegrees(angle));
        renderPart(pose, vertices, id, ModelData.EMPTY, light, overlay);
        pose.popPose();
    }

    private void renderPart(PoseStack pose, VertexConsumer vertices, ResourceLocation id, ModelData material, int light, int overlay) {
        var model = Minecraft.getInstance().getModelManager().getModel(id);
        modelRenderer.renderModel(pose.last(), vertices, null, model, 1, 1, 1, light, overlay, material, null);
    }

    private static ResourceLocation part(String name) {
        return ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/clock/" + name);
    }
}
