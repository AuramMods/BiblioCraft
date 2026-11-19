package art.arcane.bibliocraft.event;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.client.ReadingOverlay;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.TapeMeasureItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BiblioCraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRenderEvents {
    private ClientRenderEvents() {}

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            var graphics = event.getGuiGraphics();
            ReadingOverlay.draw(graphics, drawTapeDistance(graphics));
        }
    }

    private static int drawTapeDistance(GuiGraphics graphics) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (!BiblioConfig.COMMON.enableTapemeasure.get() || player == null || minecraft.screen != null
                || minecraft.options.hideGui || !minecraft.options.getCameraType().isFirstPerson()
                || minecraft.getCameraEntity() != player || player.isSpectator()) {
            return 0;
        }
        int distance = TapeMeasureItem.liveDistance(player, player.getMainHandItem());
        if (distance == 0) {
            distance = TapeMeasureItem.liveDistance(player, player.getOffhandItem());
        }
        if (distance == 0) {
            return 0;
        }
        graphics.drawCenteredString(minecraft.font, Component.translatable("overlay.bibliocraft.tape.distance", distance),
                graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 20, 0xFFFFFF);
        return minecraft.font.lineHeight;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ReadingOverlay.tick();
        }
    }

    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        // Placeholder for legacy block highlight behavior (catalog markers, item name overlays).
    }
}
