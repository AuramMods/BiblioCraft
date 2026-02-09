package art.arcane.bibliocraft.config;

import art.arcane.bibliocraft.BiblioCraft;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class BiblioFeatureToggles {
    private static final Map<ResourceLocation, BooleanSupplier> BLOCK_GATES = new HashMap<>();
    private static final Map<ResourceLocation, BooleanSupplier> ITEM_GATES = new HashMap<>();

    static {
        mapBlock("bookcase", BiblioConfig.COMMON.enableBookcase::get);
        mapBlock("bookcase_creative", BiblioConfig.COMMON.enableBookcase::get);
        mapBlock("shelf", BiblioConfig.COMMON.enableGenericshelf::get);
        mapBlock("marker_pole", BiblioConfig.COMMON.enableTapemeasure::get);
        mapBlock("clipboard", BiblioConfig.COMMON.enableClipboard::get);
        mapBlock("lantern_gold", BiblioConfig.COMMON.enableLantern::get);
        mapBlock("lantern_iron", BiblioConfig.COMMON.enableLantern::get);
        mapBlock("lamp_gold", BiblioConfig.COMMON.enableLamp::get);
        mapBlock("lamp_iron", BiblioConfig.COMMON.enableLamp::get);
        mapBlock("furniture_paneler", BiblioConfig.COMMON.enableFurniturePaneler::get);
        mapBlock("framed_chest", BiblioConfig.COMMON.enableFramedChest::get);
        mapBlock("fancy_sign", BiblioConfig.COMMON.enableFancySign::get);
        mapBlock("fancy_workbench", BiblioConfig.COMMON.enableFancyWorkbench::get);
        mapBlock("potion_shelf", BiblioConfig.COMMON.enablePotionshelf::get);
        mapBlock("tool_rack", BiblioConfig.COMMON.enableToolrack::get);
        mapBlock("label", BiblioConfig.COMMON.enableWoodLabel::get);
        mapBlock("desk", BiblioConfig.COMMON.enableWritingdesk::get);
        mapBlock("table", BiblioConfig.COMMON.enableTable::get);
        mapBlock("seat", BiblioConfig.COMMON.enableSeat::get);
        mapBlock("clock", BiblioConfig.COMMON.enableClock::get);
        mapBlock("case", BiblioConfig.COMMON.enableWeaponcase::get);
        mapBlock("map_frame", BiblioConfig.COMMON.enableMapFrame::get);
        mapBlock("painting_frame_flat", BiblioConfig.COMMON.enablePainting::get);
        mapBlock("painting_frame_simple", BiblioConfig.COMMON.enablePainting::get);
        mapBlock("painting_frame_middle", BiblioConfig.COMMON.enablePainting::get);
        mapBlock("painting_frame_fancy", BiblioConfig.COMMON.enablePainting::get);
        mapBlock("painting_frame_borderless", BiblioConfig.COMMON.enablePainting::get);
        mapBlock("painting_press", BiblioConfig.COMMON.enablePainting::get);
        mapBlock("typewriter", BiblioConfig.COMMON.enableTypewriter::get);
        mapBlock("sword_pedestal", BiblioConfig.COMMON.enableSwordPedestal::get);
        mapBlock("armor_stand", BiblioConfig.COMMON.enableArmorstand::get);
        mapBlock("bell", BiblioConfig.COMMON.enableDeskBell::get);
        mapBlock("typesetting_table", BiblioConfig.COMMON.enablePrintpressTypeMachine::get);
        mapBlock("printing_press", BiblioConfig.COMMON.enablePrintpressTypeMachine::get);
        mapBlock("cookie_jar", BiblioConfig.COMMON.enableCookieJar::get);
        mapBlock("dinner_plate", BiblioConfig.COMMON.enableDinnerPlate::get);
        mapBlock("disc_rack", BiblioConfig.COMMON.enableDiscRack::get);

        mapItem("compass", BiblioConfig.COMMON.enableWaypointCompass::get);
        mapItem("maptool", BiblioConfig.COMMON.enableMapFrame::get);
        mapItem("seatback1", BiblioConfig.COMMON.enableSeat::get);
        mapItem("seatback2", BiblioConfig.COMMON.enableSeat::get);
        mapItem("seatback3", BiblioConfig.COMMON.enableSeat::get);
        mapItem("seatback4", BiblioConfig.COMMON.enableSeat::get);
        mapItem("seatback5", BiblioConfig.COMMON.enableSeat::get);
        mapItem("stockroom_catalog", BiblioConfig.COMMON.enableStockroomCatalog::get);
        mapItem("plumb_line", BiblioConfig.COMMON.enablePlumbLine::get);
        mapItem("framing_saw", BiblioConfig.COMMON.enableFurniturePaneler::get);
        mapItem("framing_board", BiblioConfig.COMMON.enableFurniturePaneler::get);
        mapItem("framing_sheet", BiblioConfig.COMMON.enableFurniturePaneler::get);
        mapItem("tester_item", BiblioConfig.COMMON.enableTesterItem::get);
        mapItem("atlas_book", BiblioConfig.COMMON.enableAtlas::get);
        mapItem("atlas_plate", BiblioConfig.COMMON.enableAtlas::get);
        mapItem("death_compass", () -> BiblioConfig.COMMON.enableAtlas.get() && BiblioConfig.COMMON.enableDeathCompass.get());
        mapItem("painting_canvas", BiblioConfig.COMMON.enablePainting::get);
        mapItem("big_book", BiblioConfig.COMMON.enableBigBook::get);
        mapItem("recipe_book", BiblioConfig.COMMON.enableFancyWorkbench::get);
        mapItem("slotted_book", BiblioConfig.COMMON.enableSlottedBook::get);
        mapItem("hand_drill", BiblioConfig.COMMON.enableHandDrill::get);
        mapItem("tape_measure", BiblioConfig.COMMON.enableTapemeasure::get);
        mapItem("tape", BiblioConfig.COMMON.enableTapemeasure::get);
        mapItem("biblio_chase", BiblioConfig.COMMON.enablePrintpressTypeMachine::get);
        mapItem("print_plate", BiblioConfig.COMMON.enablePrintpressTypeMachine::get);
        mapItem("enchanted_plate", BiblioConfig.COMMON.enablePrintpressTypeMachine::get);
        mapItem("biblio_red_book", BiblioConfig.COMMON.enableRedstonebook::get);
        mapItem("biblio_glasses", BiblioConfig.COMMON.enableReadingglasses::get);
        mapItem("biblio_drill", BiblioConfig.COMMON.enableDrill::get);
        mapItem("biblio_creative_lock", BiblioConfig.COMMON.enableLock::get);
        mapItem("biblio_clipboard", BiblioConfig.COMMON.enableClipboard::get);
    }

    private BiblioFeatureToggles() {}

    public static boolean isBlockEnabled(@Nullable ResourceLocation blockId) {
        return evaluate(BLOCK_GATES, blockId);
    }

    public static boolean isItemEnabled(@Nullable ResourceLocation itemId) {
        return evaluate(ITEM_GATES, itemId);
    }

    public static boolean isBlockItemEnabled(@Nullable ResourceLocation blockItemId) {
        return evaluate(BLOCK_GATES, blockItemId);
    }

    public static boolean isEnchantedAtlasRecipeEnabled() {
        return BiblioConfig.COMMON.enableAtlas.get()
                && BiblioConfig.COMMON.enableDeathCompass.get()
                && BiblioConfig.COMMON.enableWaypointCompass.get();
    }

    private static boolean evaluate(Map<ResourceLocation, BooleanSupplier> gates, @Nullable ResourceLocation id) {
        if (id == null) {
            return true;
        }
        BooleanSupplier gate = gates.get(id);
        return gate == null || gate.getAsBoolean();
    }

    private static void mapBlock(String path, BooleanSupplier gate) {
        BLOCK_GATES.put(ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, path), gate);
    }

    private static void mapItem(String path, BooleanSupplier gate) {
        ITEM_GATES.put(ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, path), gate);
    }
}
