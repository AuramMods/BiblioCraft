package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.MapFrameBlock;
import art.arcane.bibliocraft.blockentity.MapFrameBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.AttachFace;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class MapFrameRenderer implements BlockEntityRenderer<MapFrameBlockEntity> {
    static final ResourceLocation PIN = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/map_frame_pin");
    private final Map<DyeColor, List<BakedQuad>> pins = new EnumMap<>(DyeColor.class);

    MapFrameRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MapFrameBlockEntity frame, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        var stack = frame.getItem(MapFrameBlockEntity.MAP);
        var level = frame.getLevel();
        if (level == null || !stack.is(Items.FILLED_MAP)) {
            return;
        }
        var state = frame.getBlockState();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(MapFrameBlock.FACING).toYRot()));
        AttachFace face = state.getValue(MapFrameBlock.FACE);
        if (face != AttachFace.WALL) {
            pose.mulPose(Axis.YP.rotationDegrees(-90));
            pose.mulPose(Axis.ZP.rotationDegrees(face == AttachFace.FLOOR ? 90 : -90));
        }
        pose.translate(-0.5, -0.5, -0.5);
        pose.pushPose();
        pose.translate(.0345, .5, .5);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(Axis.ZP.rotationDegrees(frame.rotation() * 90));
        pose.scale(1 / 128f, 1 / 128f, 1 / 128f);
        pose.translate(-64, -64, 0);
        DisplayedMapRenderer.renderPixels(pose, buffers, stack, level, true, light);
        pose.popPose();
        var vertices = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        for (var pin : frame.pins()) {
            var point = pin.point().rotate(frame.rotation());
            pose.pushPose();
            pose.translate(0, 1 - point.v(), 1 - point.u());
            for (BakedQuad quad : pins.computeIfAbsent(pin.color(), this::pinQuads)) {
                vertices.putBulkData(pose.last(), quad, 1, 1, 1, light, overlay);
            }
            pose.popPose();
        }
        pose.popPose();
    }

    private List<BakedQuad> pinQuads(DyeColor color) {
        var modelManager = Minecraft.getInstance().getModelManager();
        var pin = modelManager.getModel(PIN);
        var sprite = modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(ResourceLocation.withDefaultNamespace("block/" + color.getName() + "_wool"));
        return pin.getQuads(null, null, RandomSource.create(42)).stream()
                .map(quad -> FurnitureMaterialModel.replaceSprite(new FurnitureMaterialModel.QuadTexture(quad, sprite)))
                .toList();
    }
}
