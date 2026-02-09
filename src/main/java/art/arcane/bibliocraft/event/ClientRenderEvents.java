package art.arcane.bibliocraft.event;

import art.arcane.bibliocraft.BiblioCraft;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BiblioCraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRenderEvents {
    private ClientRenderEvents() {}

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // Placeholder for legacy GUI overlay behavior (tape measure, drill text, glasses readout).
    }

    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        // Placeholder for legacy block highlight behavior (catalog markers, item name overlays).
    }
}
