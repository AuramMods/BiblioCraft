package art.arcane.bibliocraft.event;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.client.ReadingOverlay;
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
            ReadingOverlay.draw(event.getGuiGraphics());
        }
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
