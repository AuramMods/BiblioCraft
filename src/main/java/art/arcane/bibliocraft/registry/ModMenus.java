package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.RackType;
import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.menu.ArmorStandMenu;
import art.arcane.bibliocraft.menu.BigBookMenu;
import art.arcane.bibliocraft.menu.BookcaseMenu;
import art.arcane.bibliocraft.menu.CookieJarMenu;
import art.arcane.bibliocraft.menu.DeskBookMenu;
import art.arcane.bibliocraft.menu.DeskMenu;
import art.arcane.bibliocraft.menu.DinnerPlateMenu;
import art.arcane.bibliocraft.menu.DiscRackMenu;
import art.arcane.bibliocraft.menu.DisplayCaseMenu;
import art.arcane.bibliocraft.menu.FancySignMenu;
import art.arcane.bibliocraft.menu.FancyWorkbenchMenu;
import art.arcane.bibliocraft.menu.RecipeBookMenu;
import art.arcane.bibliocraft.menu.FramedChestMenu;
import art.arcane.bibliocraft.menu.ClockMenu;
import art.arcane.bibliocraft.menu.LabelMenu;
import art.arcane.bibliocraft.menu.PlaceholderMenu;
import art.arcane.bibliocraft.menu.PrintingPressMenu;
import art.arcane.bibliocraft.menu.PaintingFrameMenu;
import art.arcane.bibliocraft.menu.PaintingPressMenu;
import art.arcane.bibliocraft.menu.MapFrameMenu;
import art.arcane.bibliocraft.menu.TypesettingTableMenu;
import art.arcane.bibliocraft.menu.RackMenu;
import art.arcane.bibliocraft.menu.ShelfMenu;
import art.arcane.bibliocraft.menu.TableMenu;
import art.arcane.bibliocraft.menu.StockroomCatalogMenu;
import art.arcane.bibliocraft.menu.WaypointCompassMenu;
import art.arcane.bibliocraft.menu.FurniturePanelerMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, BiblioCraft.MODID);

    public static final RegistryObject<MenuType<PlaceholderMenu>> ATLAS = register("atlas");
    public static final RegistryObject<MenuType<BigBookMenu>> BIG_BOOK =
            MENUS.register("big_book", () -> IForgeMenuType.create(BigBookMenu::new));
    public static final RegistryObject<MenuType<PlaceholderMenu>> SLOTTED_BOOK = register("slotted_book");
    public static final RegistryObject<MenuType<PlaceholderMenu>> TESTER = register("tester");
    public static final RegistryObject<MenuType<BookcaseMenu>> BOOKCASE =
            MENUS.register("bookcase", () -> new MenuType<>(BookcaseMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ShelfMenu>> SHELF =
            MENUS.register("shelf", () -> new MenuType<>(ShelfMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ArmorStandMenu>> ARMOR_STAND =
            MENUS.register("armor_stand", () -> new MenuType<>(ArmorStandMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<RackMenu>> POTION_SHELF = MENUS.register("potion_shelf",
            () -> new MenuType<>((id, inventory) -> new RackMenu(id, inventory, RackType.POTIONS), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<RackMenu>> TOOL_RACK = MENUS.register("tool_rack",
            () -> new MenuType<>((id, inventory) -> new RackMenu(id, inventory, RackType.TOOLS), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<DisplayCaseMenu>> CASE =
            MENUS.register("case", () -> new MenuType<>(DisplayCaseMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<LabelMenu>> LABEL =
            MENUS.register("label", () -> new MenuType<>(LabelMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<DeskMenu>> DESK =
            MENUS.register("desk", () -> new MenuType<>(DeskMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<DeskBookMenu>> DESK_BOOK =
            MENUS.register("desk_book", () -> IForgeMenuType.create(DeskBookMenu::new));
    public static final RegistryObject<MenuType<TableMenu>> TABLE =
            MENUS.register("table", () -> new MenuType<>(TableMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<CookieJarMenu>> COOKIE_JAR =
            MENUS.register("cookie_jar", () -> new MenuType<>(CookieJarMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<DinnerPlateMenu>> DINNER_PLATE =
            MENUS.register("dinner_plate", () -> new MenuType<>(DinnerPlateMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<DiscRackMenu>> DISC_RACK =
            MENUS.register("disc_rack", () -> new MenuType<>(DiscRackMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<FancySignMenu>> FANCY_SIGN =
            MENUS.register("fancy_sign", () -> IForgeMenuType.create(FancySignMenu::new));
    public static final RegistryObject<MenuType<FancyWorkbenchMenu>> FANCY_WORKBENCH =
            MENUS.register("fancy_workbench", () -> IForgeMenuType.create(FancyWorkbenchMenu::new));
    public static final RegistryObject<MenuType<RecipeBookMenu>> RECIPE_BOOK =
            MENUS.register("recipe_book", () -> IForgeMenuType.create(RecipeBookMenu::new));
    public static final RegistryObject<MenuType<PaintingPressMenu>> PAINTING_PRESS =
            MENUS.register("painting_press", () -> IForgeMenuType.create(PaintingPressMenu::new));
    public static final RegistryObject<MenuType<PaintingFrameMenu>> PAINTING =
            MENUS.register("painting", () -> IForgeMenuType.create(PaintingFrameMenu::new));
    public static final RegistryObject<MenuType<FurniturePanelerMenu>> FURNITURE_PANELER =
            MENUS.register("furniture_paneler", () -> new MenuType<>(FurniturePanelerMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<FramedChestMenu>> FRAMED_CHEST =
            MENUS.register("framed_chest", () -> IForgeMenuType.create(FramedChestMenu::new));
    public static final RegistryObject<MenuType<PlaceholderMenu>> CLIPBOARD = register("clipboard");
    public static final RegistryObject<MenuType<ClockMenu>> CLOCK =
            MENUS.register("clock", () -> IForgeMenuType.create(ClockMenu::new));
    public static final RegistryObject<MenuType<TypesettingTableMenu>> TYPESETTING_TABLE =
            MENUS.register("typesetting_table", () -> new MenuType<>(TypesettingTableMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<PrintingPressMenu>> PRINTING_PRESS =
            MENUS.register("printing_press", () -> new MenuType<>(PrintingPressMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<MapFrameMenu>> MAP_FRAME =
            MENUS.register("map_frame", () -> IForgeMenuType.create(MapFrameMenu::new));

    public static final RegistryObject<MenuType<StockroomCatalogMenu>> STOCKROOM_CATALOG =
            MENUS.register("stockroom_catalog", () -> IForgeMenuType.create(StockroomCatalogMenu::new));
    public static final RegistryObject<MenuType<WaypointCompassMenu>> WAYPOINT_COMPASS =
            MENUS.register("compass", () -> IForgeMenuType.create(WaypointCompassMenu::new));

    private static final Map<ResourceLocation, RegistryObject<MenuType<PlaceholderMenu>>> BLOCK_MENU_MAP = new HashMap<>();

    static {
        mapBlock("clipboard", CLIPBOARD);
    }

    private ModMenus() {}

    private static RegistryObject<MenuType<PlaceholderMenu>> register(String name) {
        @SuppressWarnings("unchecked")
        final RegistryObject<MenuType<PlaceholderMenu>>[] holder = new RegistryObject[1];

        holder[0] = MENUS.register(name,
                () -> IForgeMenuType.create((containerId, inventory, buffer) -> new PlaceholderMenu(holder[0].get(), containerId)));

        return holder[0];
    }

    @Nullable
    public static MenuType<PlaceholderMenu> getMenuForBlock(ResourceLocation blockId) {
        if (!BiblioFeatureToggles.isBlockEnabled(blockId)) {
            return null;
        }
        RegistryObject<MenuType<PlaceholderMenu>> entry = BLOCK_MENU_MAP.get(blockId);
        return entry == null ? null : entry.get();
    }

    private static void mapBlock(String blockPath, RegistryObject<MenuType<PlaceholderMenu>> menuType) {
        BLOCK_MENU_MAP.put(ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, blockPath), menuType);
    }
}
