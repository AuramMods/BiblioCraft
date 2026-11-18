package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.BookcaseBlockEntity;
import art.arcane.bibliocraft.blockentity.CookieJarBlockEntity;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
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
        event.registerBlockEntityRenderer(ModBlockEntities.CASE.get(), DisplayCaseRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DINNER_PLATE.get(), DinnerPlateRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DISC_RACK.get(), DiscRackRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.SHELF.get(), ShelfScreen::new);
            MenuScreens.register(ModMenus.BOOKCASE.get(), BookcaseScreen::new);
            MenuScreens.register(ModMenus.POTION_SHELF.get(), RackScreen::new);
            MenuScreens.register(ModMenus.TOOL_RACK.get(), RackScreen::new);
            MenuScreens.register(ModMenus.CASE.get(), DisplayCaseScreen::new);
            MenuScreens.register(ModMenus.COOKIE_JAR.get(), CookieJarScreen::new);
            MenuScreens.register(ModMenus.DINNER_PLATE.get(), DinnerPlateScreen::new);
            MenuScreens.register(ModMenus.DISC_RACK.get(), DiscRackScreen::new);
            MenuScreens.register(ModMenus.FURNITURE_PANELER.get(), FurniturePanelerScreen::new);
        });
    }

    @SubscribeEvent
    public static void wrapFurnitureModels(ModelEvent.ModifyBakingResult event) {
        var furniture = Set.of(ModBlocks.SHELF.getId().getPath(), ModBlocks.FURNITURE_PANELER.getId().getPath(),
                ModBlocks.BOOKCASE.getId().getPath(), ModBlocks.BOOKCASE_CREATIVE.getId().getPath(),
                ModBlocks.TOOL_RACK.getId().getPath(), ModBlocks.POTION_SHELF.getId().getPath(),
                ModBlocks.CASE.getId().getPath());
        var bookcases = Set.of(ModBlocks.BOOKCASE.getId().getPath(), ModBlocks.BOOKCASE_CREATIVE.getId().getPath());
        var books = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/bookcase_books");
        var cookies = ResourceLocation.withDefaultNamespace("item/cookie");
        event.getModels().replaceAll((id, model) -> {
            if (!(id instanceof ModelResourceLocation location) || !id.getNamespace().equals(BiblioCraft.MODID)) {
                return model;
            }
            String path = id.getPath();
            if (furniture.contains(path)) {
                model = new FurnitureMaterialModel(model);
            }
            if ("inventory".equals(location.getVariant())) {
                return model;
            }
            if (bookcases.contains(path)) {
                return new SlotVisibilityModel(model, books, BookcaseBlockEntity.OCCUPIED_SLOTS);
            }
            if (path.equals(ModBlocks.COOKIE_JAR.getId().getPath())) {
                return new SlotVisibilityModel(model, cookies, CookieJarBlockEntity.VISIBLE_COOKIES);
            }
            return model;
        });
    }
}
