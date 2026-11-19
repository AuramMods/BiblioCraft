package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.map.AtlasContents;
import art.arcane.bibliocraft.map.MapPin;
import art.arcane.bibliocraft.registry.ModItems;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = BiblioCraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
final class AtlasHandRenderer {
    private static final ResourceLocation COVER = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "item/atlas_open_cover");
    private static final ResourceLocation PIN = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "item/atlas_open_pin");
    private static final Map<DyeColor, List<BakedQuad>> PIN_COLORS = new EnumMap<>(DyeColor.class);
    private static BakedModel lastPinModel;

    private AtlasHandRenderer() {}

    static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(COVER);
        event.register(PIN);
    }

    @SubscribeEvent
    public static void render(RenderHandEvent event) {
        if (!event.getItemStack().is(ModItems.ATLAS_BOOK.get()) || !BiblioConfig.COMMON.enableAtlas.get()) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        event.setCanceled(true);
        var pose = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        var models = minecraft.getModelManager();
        boolean offhand = event.getHand() == InteractionHand.OFF_HAND;
        boolean small = offhand || !minecraft.player.getOffhandItem().isEmpty();
        boolean right = (minecraft.player.getMainArm() == HumanoidArm.RIGHT) != offhand;
        float pitch = small ? 0 : .5f - .5f * Mth.cos((float) Math.PI * Mth.clamp(1.1f - event.getInterpolatedPitch() / 45f, 0, 1));
        float swing = event.getSwingProgress();
        pose.pushPose();
        pose.translate(0, .04 + .1 * Mth.sin(swing * (float) Math.PI) - 1.2 * event.getEquipProgress() - .5 * pitch,
                -.72 - .4 * Mth.sin(Mth.sqrt(swing) * (float) Math.PI));
        pose.mulPose(Axis.XP.rotationDegrees(-85 * pitch));
        pose.translate(.38, .38, -.02);
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        if (small) {
            pose.scale(.489f, .489f, .489f);
            pose.translate(0, -.885, right ? -.865 : 1.66);
        }
        pose.scale(.76f, .76f, .76f);
        var vertices = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        for (var quad : models.getModel(COVER).getQuads(null, null, RandomSource.create(42))) {
            vertices.putBulkData(pose.last(), quad, 1, 1, 1, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
        var map = AtlasContents.displayedMap(event.getItemStack());
        pose.pushPose();
        pose.translate(.029, 0, 1);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.scale(1 / 128f, 1 / 128f, 1 / 128f);
        DisplayedMapRenderer.renderBackground(pose, buffers, LightTexture.FULL_BRIGHT);
        pose.translate(0, 0, -.3);
        DisplayedMapRenderer.renderPixels(pose, buffers, map, minecraft.level, false, LightTexture.FULL_BRIGHT);
        pose.popPose();
        BakedModel pinModel = models.getModel(PIN);
        if (pinModel != lastPinModel) {
            lastPinModel = pinModel;
            PIN_COLORS.clear();
        }
        vertices = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        for (var pin : MapPin.read(map)) {
            pose.pushPose();
            pose.translate(0, 1 - pin.point().v(), 1 - pin.point().u());
            var quads = PIN_COLORS.computeIfAbsent(pin.color(), color -> {
                var sprite = models.getAtlas(TextureAtlas.LOCATION_BLOCKS)
                        .getSprite(ResourceLocation.withDefaultNamespace("block/" + color.getName() + "_wool"));
                return pinModel.getQuads(null, null, RandomSource.create(42)).stream()
                        .map(quad -> FurnitureMaterialModel.replaceSprite(new FurnitureMaterialModel.QuadTexture(quad, sprite)))
                        .toList();
            });
            for (var quad : quads) {
                vertices.putBulkData(pose.last(), quad, 1, 1, 1, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
        }
        pose.popPose();
    }
}
