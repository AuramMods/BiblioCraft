package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.SeatBlock;
import art.arcane.bibliocraft.block.TableBlock;
import art.arcane.bibliocraft.blockentity.BookcaseBlockEntity;
import art.arcane.bibliocraft.blockentity.CookieJarBlockEntity;
import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import art.arcane.bibliocraft.blockentity.FancyWorkbenchBlockEntity;
import art.arcane.bibliocraft.blockentity.PrintingPressBlockEntity;
import art.arcane.bibliocraft.blockentity.PaintingPressBlockEntity;
import art.arcane.bibliocraft.blockentity.SeatBlockEntity;
import art.arcane.bibliocraft.blockentity.TableBlockEntity;
import art.arcane.bibliocraft.blockentity.TypesettingTableBlockEntity;
import art.arcane.bibliocraft.item.ColoredBlockItem;
import art.arcane.bibliocraft.item.ReadingGlassesItem;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModEntities;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import art.arcane.bibliocraft.registry.ModParticles;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Set;

@Mod.EventBusSubscriber(modid = BiblioCraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEAT_ENTITY.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SHELF.get(), ShelfRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FURNITURE_PANELER.get(), FurniturePanelerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.POTION_SHELF.get(), RackRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TOOL_RACK.get(), RackRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CASE.get(), DisplayCaseRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DINNER_PLATE.get(), DinnerPlateRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DISC_RACK.get(), DiscRackRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SWORD_PEDESTAL.get(), SwordPedestalRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TYPEWRITER.get(), TypewriterRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DESK.get(), DeskRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ARMOR_STAND.get(), ArmorStandRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TABLE.get(), TableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LABEL.get(), SignDisplayRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FANCY_SIGN.get(), SignDisplayRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CLOCK.get(), ClockRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FRAMED_CHEST.get(), FramedChestRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PRINTING_PRESS.get(), PrintingPressRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PAINTING_PRESS.get(), PaintingPressRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PAINTING_FRAME_BORDERLESS.get(), PaintingFrameRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PAINTING_FRAME_FLAT.get(), PaintingFrameRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PAINTING_FRAME_SIMPLE.get(), PaintingFrameRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PAINTING_FRAME_MIDDLE.get(), PaintingFrameRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PAINTING_FRAME_FANCY.get(), PaintingFrameRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MAP_FRAME.get(), MapFrameRenderer::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        TypewriterRenderer.registerModels(event);
        ClockRenderer.registerModels(event);
        FramedChestRenderer.registerModels(event);
        PrintingPressRenderer.registerModels(event);
        event.register(PaintingPressRenderer.LID);
        event.register(MapFrameRenderer.PIN);
        AtlasHandRenderer.registerModels(event);
    }

    @SubscribeEvent
    public static void paintingResources(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ClientPaintingArt());
    }

    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("waypoint_compass", (IGeometryLoader<WaypointCompassModel>) WaypointCompassModel::read);
        event.register("atlas", (IGeometryLoader<AtlasModel>) AtlasModel::read);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.CATALOG.get(), CatalogParticle.Provider::new);
    }

    @SubscribeEvent
    public static void setupClient(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.SHELF.get(), ShelfScreen::new);
            MenuScreens.register(ModMenus.BOOKCASE.get(), BookcaseScreen::new);
            MenuScreens.register(ModMenus.POTION_SHELF.get(), RackScreen::new);
            MenuScreens.register(ModMenus.TOOL_RACK.get(), RackScreen::new);
            MenuScreens.register(ModMenus.CASE.get(), DisplayCaseScreen::new);
            MenuScreens.register(ModMenus.COOKIE_JAR.get(), CookieJarScreen::new);
            MenuScreens.register(ModMenus.DINNER_PLATE.get(), DinnerPlateScreen::new);
            MenuScreens.register(ModMenus.DISC_RACK.get(), DiscRackScreen::new);
            MenuScreens.register(ModMenus.DESK.get(), DeskScreen::new);
            MenuScreens.register(ModMenus.DESK_BOOK.get(), DeskBookScreen::new);
            MenuScreens.register(ModMenus.BIG_BOOK.get(), BigBookScreen::new);
            MenuScreens.register(ModMenus.ARMOR_STAND.get(), ArmorStandScreen::new);
            MenuScreens.register(ModMenus.TABLE.get(), TableScreen::new);
            MenuScreens.register(ModMenus.LABEL.get(), LabelScreen::new);
            MenuScreens.register(ModMenus.FANCY_SIGN.get(), FancySignScreen::new);
            MenuScreens.register(ModMenus.CLOCK.get(), ClockScreen::new);
            MenuScreens.register(ModMenus.STOCKROOM_CATALOG.get(), StockroomCatalogScreen::new);
            MenuScreens.register(ModMenus.WAYPOINT_COMPASS.get(), WaypointCompassScreen::new);
            MenuScreens.register(ModMenus.FRAMED_CHEST.get(), FramedChestScreen::new);
            MenuScreens.register(ModMenus.FANCY_WORKBENCH.get(), FancyWorkbenchScreen::new);
            MenuScreens.register(ModMenus.RECIPE_BOOK.get(), RecipeBookScreen::new);
            MenuScreens.register(ModMenus.FURNITURE_PANELER.get(), FurniturePanelerScreen::new);
            MenuScreens.register(ModMenus.TYPESETTING_TABLE.get(), TypesettingTableScreen::new);
            MenuScreens.register(ModMenus.PRINTING_PRESS.get(), PrintingPressScreen::new);
            MenuScreens.register(ModMenus.PAINTING_PRESS.get(), PaintingPressScreen::new);
            MenuScreens.register(ModMenus.PAINTING.get(), PaintingFrameScreen::new);
            MenuScreens.register(ModMenus.MAP_FRAME.get(), MapFrameScreen::new);
            MenuScreens.register(ModMenus.ATLAS.get(), AtlasScreen::new);
            ItemProperties.register(ModItems.BIBLIO_GLASSES.get(),
                    ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "glasses_variant"),
                    (stack, level, entity, seed) -> ReadingGlassesItem.variant(stack).id());
            var colorProperty = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "furniture_color");
            ModBlocks.getTabItems().forEach(entry -> {
                if (entry.get() instanceof ColoredBlockItem colored) {
                    ItemProperties.register(colored, colorProperty,
                            (stack, level, entity, seed) -> ColoredBlockItem.getColor(stack).getId());
                }
            });
        });
    }

    @SubscribeEvent
    public static void wrapFurnitureModels(ModelEvent.ModifyBakingResult event) {
        var furniture = Set.of(ModBlocks.SHELF.getId().getPath(), ModBlocks.FURNITURE_PANELER.getId().getPath(),
                ModBlocks.BOOKCASE.getId().getPath(), ModBlocks.BOOKCASE_CREATIVE.getId().getPath(),
                ModBlocks.TOOL_RACK.getId().getPath(), ModBlocks.POTION_SHELF.getId().getPath(),
                ModBlocks.CASE.getId().getPath(), ModBlocks.DESK.getId().getPath(), ModBlocks.ARMOR_STAND.getId().getPath(),
                ModBlocks.TABLE.getId().getPath(), ModBlocks.LABEL.getId().getPath(), ModBlocks.FANCY_SIGN.getId().getPath(),
                ModBlocks.CLOCK.getId().getPath(), ModBlocks.FRAMED_CHEST.getId().getPath(),
                ModBlocks.FANCY_WORKBENCH.getId().getPath());
        var bookcases = Set.of(ModBlocks.BOOKCASE.getId().getPath(), ModBlocks.BOOKCASE_CREATIVE.getId().getPath());
        var seatbacks = Set.of(ModItems.SEAT_BACK_1.getId().getPath(), ModItems.SEAT_BACK_2.getId().getPath(),
                ModItems.SEAT_BACK_3.getId().getPath(), ModItems.SEAT_BACK_4.getId().getPath(), ModItems.SEAT_BACK_5.getId().getPath());
        var books = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/bookcase_books");
        var cookies = ResourceLocation.withDefaultNamespace("item/cookie");
        var deskBooks = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/desk_books");
        var deskCarpet = ResourceLocation.withDefaultNamespace("block/white_wool");
        var typesetting = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/typesettingtable");
        event.getModels().replaceAll((id, model) -> {
            if (id.equals(ClockRenderer.GRANDFATHER_CASE) || FramedChestRenderer.isLid(id)) {
                return new FurnitureMaterialModel(model);
            }
            if (!(id instanceof ModelResourceLocation location) || !id.getNamespace().equals(BiblioCraft.MODID)) {
                return model;
            }
            String path = id.getPath();
            if (path.equals(ModBlocks.MAP_FRAME.getId().getPath())) {
                return "inventory".equals(location.getVariant()) ? new FurnitureMaterialModel(model)
                        : new MapFrameModel(model);
            }
            if (path.startsWith("painting_frame_")) {
                return "inventory".equals(location.getVariant()) ? new FurnitureMaterialModel(model)
                        : new PaintingFrameModel(model);
            }
            if (path.equals(ModItems.PAINTING_CANVAS.getId().getPath()) && "inventory".equals(location.getVariant())) {
                return new PaintingCanvasModel(model);
            }
            if (path.equals(ModBlocks.DESK.getId().getPath()) && !"inventory".equals(location.getVariant())) {
                model = new SlotVisibilityModel(model, deskBooks, DeskBlockEntity.VISIBLE_PARTS);
                model = new SlotVisibilityModel(model, deskCarpet, DeskBlockEntity.VISIBLE_PARTS);
            }
            if (path.equals(ModBlocks.TABLE.getId().getPath()) && !"inventory".equals(location.getVariant())) {
                model = new SlotVisibilityModel(model, (state, data) -> TableBlock.visibleParts(state,
                        data.has(TableBlockEntity.CLOTH_COLOR), data.has(TableBlockEntity.CARPET_COLOR)));
            }
            if (path.equals(ModBlocks.SEAT.getId().getPath())) {
                if (!"inventory".equals(location.getVariant())) {
                    model = new SlotVisibilityModel(model, (state, data) -> {
                        var appearance = data.get(SeatBlockEntity.APPEARANCE);
                        return SeatBlock.visibleParts(state, appearance == null ? 0 : appearance.back(),
                                appearance != null && appearance.carpet() != null);
                    });
                }
                return new FurnitureMaterialModel(model, true);
            }
            if (seatbacks.contains(path) && "inventory".equals(location.getVariant())) {
                return new FurnitureMaterialModel(model, true);
            }
            if (furniture.contains(path)) {
                model = new FurnitureMaterialModel(model);
            }
            if ("inventory".equals(location.getVariant())) {
                return model;
            }
            if (path.equals(ModBlocks.FANCY_WORKBENCH.getId().getPath())) {
                return new SlotVisibilityModel(model, books, FancyWorkbenchBlockEntity.OCCUPIED_BOOKS);
            }
            if (bookcases.contains(path)) {
                return new SlotVisibilityModel(model, books, BookcaseBlockEntity.OCCUPIED_SLOTS);
            }
            if (path.equals(ModBlocks.COOKIE_JAR.getId().getPath())) {
                return new SlotVisibilityModel(model, cookies, CookieJarBlockEntity.VISIBLE_COOKIES);
            }
            if (path.equals(ModBlocks.TYPESETTING_TABLE.getId().getPath())) {
                return new SlotVisibilityModel(model, typesetting, TypesettingTableBlockEntity.VISIBLE_PARTS);
            }
            if (path.equals(ModBlocks.PRINTING_PRESS.getId().getPath())) {
                return new PrintingPressModel(model);
            }
            if (path.equals(ModBlocks.PAINTING_PRESS.getId().getPath())) {
                return new SlotVisibilityModel(model, null, PaintingPressBlockEntity.VISIBLE_PARTS);
            }
            return model;
        });
    }
}
