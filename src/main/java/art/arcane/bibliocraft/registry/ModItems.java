package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.item.BigBookItem;
import art.arcane.bibliocraft.item.FramingSawItem;
import art.arcane.bibliocraft.item.FurnitureDrillItem;
import art.arcane.bibliocraft.item.FurnitureLockItem;
import art.arcane.bibliocraft.item.MenuOpeningItem;
import art.arcane.bibliocraft.item.PrintingPlateItem;
import art.arcane.bibliocraft.item.PaintingCanvasItem;
import art.arcane.bibliocraft.item.RecipeBookItem;
import art.arcane.bibliocraft.item.SeatBackItem;
import art.arcane.bibliocraft.item.StockroomCatalogItem;
import art.arcane.bibliocraft.item.WaypointCompassItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BiblioCraft.MODID);

    private static final List<RegistryObject<Item>> TAB_ITEMS = new ArrayList<>();

    public static final RegistryObject<Item> WAYPOINT_COMPASS = register("compass", WaypointCompassItem::new);
    public static final RegistryObject<Item> MAP_TOOL = register("maptool");
    public static final RegistryObject<Item> SEAT_BACK_1 = register("seatback1", () -> new SeatBackItem(1));
    public static final RegistryObject<Item> SEAT_BACK_2 = register("seatback2", () -> new SeatBackItem(2));
    public static final RegistryObject<Item> SEAT_BACK_3 = register("seatback3", () -> new SeatBackItem(3));
    public static final RegistryObject<Item> SEAT_BACK_4 = register("seatback4", () -> new SeatBackItem(4));
    public static final RegistryObject<Item> SEAT_BACK_5 = register("seatback5", () -> new SeatBackItem(5));
    public static final RegistryObject<Item> STOCKROOM_CATALOG = register("stockroom_catalog", StockroomCatalogItem::new);
    public static final RegistryObject<Item> PLUMB_LINE = register("plumb_line");
    public static final RegistryObject<Item> FRAMING_SAW = register("framing_saw", FramingSawItem::new);
    public static final RegistryObject<Item> FRAMING_BOARD = register("framing_board");
    public static final RegistryObject<Item> FRAMING_SHEET = register("framing_sheet");
    public static final RegistryObject<Item> TESTER_ITEM = register(
            "tester_item",
            () -> new MenuOpeningItem(new Item.Properties(), ModMenus.TESTER::get, "tester")
    );
    public static final RegistryObject<Item> ATLAS_BOOK = register(
            "atlas_book",
            () -> new MenuOpeningItem(new Item.Properties().stacksTo(1), ModMenus.ATLAS::get, "atlas")
    );
    public static final RegistryObject<Item> ATLAS_PLATE = register("atlas_plate");
    public static final RegistryObject<Item> DEATH_COMPASS = register("death_compass");
    public static final RegistryObject<Item> PAINTING_CANVAS = register("painting_canvas", PaintingCanvasItem::new);
    public static final RegistryObject<Item> BIG_BOOK = register("big_book", BigBookItem::new);
    public static final RegistryObject<Item> RECIPE_BOOK = register("recipe_book", RecipeBookItem::new);
    public static final RegistryObject<Item> SLOTTED_BOOK = register(
            "slotted_book",
            () -> new MenuOpeningItem(new Item.Properties().stacksTo(1), ModMenus.SLOTTED_BOOK::get, "slotted_book")
    );
    public static final RegistryObject<Item> HAND_DRILL =
            register("hand_drill", () -> new FurnitureDrillItem(new Item.Properties().stacksTo(1), true));
    public static final RegistryObject<Item> TAPE_MEASURE = register("tape_measure");
    public static final RegistryObject<Item> TAPE = register("tape");
    public static final RegistryObject<Item> BIBLIO_CHASE = register("biblio_chase");
    public static final RegistryObject<Item> PRINT_PLATE = register("print_plate", PrintingPlateItem::new);
    public static final RegistryObject<Item> ENCHANTED_PLATE = register("enchanted_plate", PrintingPlateItem::new);
    public static final RegistryObject<Item> BIBLIO_RED_BOOK = register("biblio_red_book");
    public static final RegistryObject<Item> BIBLIO_GLASSES = register("biblio_glasses");
    public static final RegistryObject<Item> BIBLIO_DRILL =
            register("biblio_drill", () -> new FurnitureDrillItem(new Item.Properties().stacksTo(1), false));
    public static final RegistryObject<Item> BIBLIO_CREATIVE_LOCK =
            register("biblio_creative_lock", FurnitureLockItem::new);
    public static final RegistryObject<Item> BIBLIO_CLIPBOARD = register("biblio_clipboard");

    private ModItems() {}

    public static List<RegistryObject<Item>> getTabItems() {
        return Collections.unmodifiableList(TAB_ITEMS);
    }

    private static RegistryObject<Item> register(String name) {
        return register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> register(String name, java.util.function.Supplier<Item> supplier) {
        RegistryObject<Item> item = ITEMS.register(name, supplier);
        TAB_ITEMS.add(item);
        return item;
    }
}
