package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.menu.PlaceholderMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
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
    public static final RegistryObject<MenuType<PlaceholderMenu>> SLOTTED_BOOK = register("slotted_book");
    public static final RegistryObject<MenuType<PlaceholderMenu>> TESTER = register("tester");
    public static final RegistryObject<MenuType<PlaceholderMenu>> BOOKCASE = register("bookcase");
    public static final RegistryObject<MenuType<PlaceholderMenu>> SHELF = register("shelf");
    public static final RegistryObject<MenuType<PlaceholderMenu>> ARMOR_STAND = register("armor_stand");
    public static final RegistryObject<MenuType<PlaceholderMenu>> POTION_SHELF = register("potion_shelf");
    public static final RegistryObject<MenuType<PlaceholderMenu>> TOOL_RACK = register("tool_rack");
    public static final RegistryObject<MenuType<PlaceholderMenu>> CASE = register("case");
    public static final RegistryObject<MenuType<PlaceholderMenu>> LABEL = register("label");
    public static final RegistryObject<MenuType<PlaceholderMenu>> DESK = register("desk");
    public static final RegistryObject<MenuType<PlaceholderMenu>> TABLE = register("table");
    public static final RegistryObject<MenuType<PlaceholderMenu>> COOKIE_JAR = register("cookie_jar");
    public static final RegistryObject<MenuType<PlaceholderMenu>> DINNER_PLATE = register("dinner_plate");
    public static final RegistryObject<MenuType<PlaceholderMenu>> DISC_RACK = register("disc_rack");
    public static final RegistryObject<MenuType<PlaceholderMenu>> FANCY_SIGN = register("fancy_sign");
    public static final RegistryObject<MenuType<PlaceholderMenu>> FANCY_WORKBENCH = register("fancy_workbench");
    public static final RegistryObject<MenuType<PlaceholderMenu>> PAINTING_PRESS = register("painting_press");
    public static final RegistryObject<MenuType<PlaceholderMenu>> PAINTING = register("painting");
    public static final RegistryObject<MenuType<PlaceholderMenu>> FURNITURE_PANELER = register("furniture_paneler");
    public static final RegistryObject<MenuType<PlaceholderMenu>> FRAMED_CHEST = register("framed_chest");
    public static final RegistryObject<MenuType<PlaceholderMenu>> CLIPBOARD = register("clipboard");
    public static final RegistryObject<MenuType<PlaceholderMenu>> CLOCK = register("clock");
    public static final RegistryObject<MenuType<PlaceholderMenu>> TYPESETTING_TABLE = register("typesetting_table");
    public static final RegistryObject<MenuType<PlaceholderMenu>> PRINTING_PRESS = register("printing_press");
    public static final RegistryObject<MenuType<PlaceholderMenu>> MAP_FRAME = register("map_frame");

    private static final Map<ResourceLocation, RegistryObject<MenuType<PlaceholderMenu>>> BLOCK_MENU_MAP = new HashMap<>();

    static {
        mapBlock("bookcase", BOOKCASE);
        mapBlock("bookcase_creative", BOOKCASE);
        mapBlock("shelf", SHELF);
        mapBlock("armor_stand", ARMOR_STAND);
        mapBlock("potion_shelf", POTION_SHELF);
        mapBlock("tool_rack", TOOL_RACK);
        mapBlock("case", CASE);
        mapBlock("label", LABEL);
        mapBlock("desk", DESK);
        mapBlock("table", TABLE);
        mapBlock("cookie_jar", COOKIE_JAR);
        mapBlock("dinner_plate", DINNER_PLATE);
        mapBlock("disc_rack", DISC_RACK);
        mapBlock("fancy_sign", FANCY_SIGN);
        mapBlock("fancy_workbench", FANCY_WORKBENCH);
        mapBlock("painting_press", PAINTING_PRESS);
        mapBlock("painting_frame_borderless", PAINTING);
        mapBlock("painting_frame_flat", PAINTING);
        mapBlock("painting_frame_simple", PAINTING);
        mapBlock("painting_frame_middle", PAINTING);
        mapBlock("painting_frame_fancy", PAINTING);
        mapBlock("furniture_paneler", FURNITURE_PANELER);
        mapBlock("framed_chest", FRAMED_CHEST);
        mapBlock("clipboard", CLIPBOARD);
        mapBlock("clock", CLOCK);
        mapBlock("typesetting_table", TYPESETTING_TABLE);
        mapBlock("printing_press", PRINTING_PRESS);
        mapBlock("map_frame", MAP_FRAME);
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
        RegistryObject<MenuType<PlaceholderMenu>> entry = BLOCK_MENU_MAP.get(blockId);
        return entry == null ? null : entry.get();
    }

    private static void mapBlock(String blockPath, RegistryObject<MenuType<PlaceholderMenu>> menuType) {
        BLOCK_MENU_MAP.put(ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, blockPath), menuType);
    }
}
