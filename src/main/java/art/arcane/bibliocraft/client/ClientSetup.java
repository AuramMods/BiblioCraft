package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Set;

@Mod.EventBusSubscriber(modid = BiblioCraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SHELF.get(), ShelfRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FURNITURE_PANELER.get(), FurniturePanelerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.POTION_SHELF.get(), RackRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TOOL_RACK.get(), RackRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.SHELF.get(), ShelfScreen::new);
            MenuScreens.register(ModMenus.BOOKCASE.get(), BookcaseScreen::new);
            MenuScreens.register(ModMenus.POTION_SHELF.get(), RackScreen::new);
            MenuScreens.register(ModMenus.TOOL_RACK.get(), RackScreen::new);
            MenuScreens.register(ModMenus.FURNITURE_PANELER.get(), FurniturePanelerScreen::new);
        });
    }

    @SubscribeEvent
    public static void wrapFurnitureModels(ModelEvent.ModifyBakingResult event) {
        var furniture = Set.of(ModBlocks.SHELF.getId().getPath(), ModBlocks.FURNITURE_PANELER.getId().getPath(),
                ModBlocks.BOOKCASE.getId().getPath(), ModBlocks.BOOKCASE_CREATIVE.getId().getPath(),
                ModBlocks.TOOL_RACK.getId().getPath(), ModBlocks.POTION_SHELF.getId().getPath());
        event.getModels().replaceAll((id, model) -> id instanceof ModelResourceLocation
                && id.getNamespace().equals(BiblioCraft.MODID) && furniture.contains(id.getPath())
                ? new FurnitureMaterialModel(model) : model);
    }
}
