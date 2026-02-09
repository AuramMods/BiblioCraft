package art.arcane.bibliocraft.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BiblioConfig {
    public static final ForgeConfigSpec SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        SPEC = builder.build();
    }

    private BiblioConfig() {}

    public static final class Common {
        public final ForgeConfigSpec.BooleanValue enableBookcase;
        public final ForgeConfigSpec.BooleanValue enableArmorstand;
        public final ForgeConfigSpec.BooleanValue enablePotionshelf;
        public final ForgeConfigSpec.BooleanValue enableToolrack;
        public final ForgeConfigSpec.BooleanValue enableWeaponcase;
        public final ForgeConfigSpec.BooleanValue enableGenericshelf;
        public final ForgeConfigSpec.BooleanValue enableWoodLabel;
        public final ForgeConfigSpec.BooleanValue enableWritingdesk;
        public final ForgeConfigSpec.BooleanValue enableTable;
        public final ForgeConfigSpec.BooleanValue enablePrintpressTypeMachine;
        public final ForgeConfigSpec.BooleanValue enableLamp;
        public final ForgeConfigSpec.BooleanValue enableLantern;
        public final ForgeConfigSpec.BooleanValue enableCookieJar;
        public final ForgeConfigSpec.BooleanValue enableDinnerPlate;
        public final ForgeConfigSpec.BooleanValue enableDiscRack;
        public final ForgeConfigSpec.BooleanValue enableMapFrame;
        public final ForgeConfigSpec.BooleanValue enableSeat;
        public final ForgeConfigSpec.BooleanValue enableRedstonebook;
        public final ForgeConfigSpec.BooleanValue enableReadingglasses;
        public final ForgeConfigSpec.BooleanValue enableTapemeasure;
        public final ForgeConfigSpec.BooleanValue enableDrill;
        public final ForgeConfigSpec.BooleanValue enableLock;
        public final ForgeConfigSpec.BooleanValue enableClipboard;
        public final ForgeConfigSpec.BooleanValue enableWaypointCompass;
        public final ForgeConfigSpec.BooleanValue enableSwordPedestal;
        public final ForgeConfigSpec.BooleanValue enableFancyWorkbench;
        public final ForgeConfigSpec.BooleanValue enableFancySign;
        public final ForgeConfigSpec.BooleanValue enableBigBook;
        public final ForgeConfigSpec.BooleanValue enableSlottedBook;
        public final ForgeConfigSpec.BooleanValue enableDeskBell;
        public final ForgeConfigSpec.BooleanValue enableHandDrill;
        public final ForgeConfigSpec.BooleanValue enableTypewriter;
        public final ForgeConfigSpec.BooleanValue enableClock;
        public final ForgeConfigSpec.BooleanValue enablePainting;
        public final ForgeConfigSpec.BooleanValue enableTesterItem;
        public final ForgeConfigSpec.BooleanValue enableAtlas;
        public final ForgeConfigSpec.BooleanValue enableDeathCompass;
        public final ForgeConfigSpec.BooleanValue enableFurniturePaneler;
        public final ForgeConfigSpec.BooleanValue enablePlumbLine;
        public final ForgeConfigSpec.BooleanValue enableFramedChest;
        public final ForgeConfigSpec.BooleanValue enableStockroomCatalog;

        public final ForgeConfigSpec.BooleanValue enableLockRecipe;
        public final ForgeConfigSpec.BooleanValue enableRecipeBookCrafting;
        public final ForgeConfigSpec.BooleanValue enablePublicTypesettingBooks;

        public final ForgeConfigSpec.BooleanValue forceFastRenderShelf;
        public final ForgeConfigSpec.BooleanValue forceFastRenderPotionShelf;
        public final ForgeConfigSpec.BooleanValue forceFastRenderFancySign;
        public final ForgeConfigSpec.BooleanValue forceFastRenderLabel;
        public final ForgeConfigSpec.BooleanValue forceFastRenderTabel;
        public final ForgeConfigSpec.BooleanValue forceFastRenderCase;
        public final ForgeConfigSpec.BooleanValue forceFastRenderToolrack;
        public final ForgeConfigSpec.BooleanValue forceFastRenderDinnerPlate;
        public final ForgeConfigSpec.BooleanValue forceFastRenderDiscRack;

        public final ForgeConfigSpec.BooleanValue emitLight;
        public final ForgeConfigSpec.BooleanValue disablerenderers;
        public final ForgeConfigSpec.BooleanValue chairRedstone;
        public final ForgeConfigSpec.BooleanValue checkforupdate;

        public final ForgeConfigSpec.IntValue enchPlateMaxUses;
        public final ForgeConfigSpec.IntValue enchantmentMultiplyer;
        public final ForgeConfigSpec.IntValue mapUpdateRate;
        public final ForgeConfigSpec.IntValue defaultBigBookTextScale;

        public final ForgeConfigSpec.DoubleValue renderDistancePainting;

        public final ForgeConfigSpec.ConfigValue<String> allowedBooks;
        public final ForgeConfigSpec.ConfigValue<String> additionalTools;
        public final ForgeConfigSpec.ConfigValue<String> additionalPotions;
        public final ForgeConfigSpec.ConfigValue<String> additionalDiscs;

        private Common(ForgeConfigSpec.Builder builder) {
            builder.push("blocks_enabled");
            enableBookcase = define(builder, "bookcase", true);
            enableArmorstand = define(builder, "armorstand", true);
            enablePotionshelf = define(builder, "potionshelf", true);
            enableToolrack = define(builder, "toolrack", true);
            enableWeaponcase = define(builder, "weaponcase", true);
            enableGenericshelf = define(builder, "genericshelf", true);
            enableWoodLabel = define(builder, "wood_label", true);
            enableWritingdesk = define(builder, "writingdesk", true);
            enableTable = define(builder, "table", true);
            enablePrintpressTypeMachine = define(builder, "printpress_type_machine", true);
            enableLamp = define(builder, "lamp", true);
            enableLantern = define(builder, "lantern", true);
            enableCookieJar = define(builder, "cookie_jar", true);
            enableDinnerPlate = define(builder, "dinner_plate", true);
            enableDiscRack = define(builder, "disc_rack", true);
            enableMapFrame = define(builder, "map_frame", true);
            enableSeat = define(builder, "seat", true);
            enableRedstonebook = define(builder, "redstonebook", true);
            enableReadingglasses = define(builder, "reading_glasses", true);
            enableTapemeasure = define(builder, "tape_measure", true);
            enableDrill = define(builder, "drill", true);
            enableLock = define(builder, "lock_and_key", true);
            enableClipboard = define(builder, "clipboard", true);
            enableWaypointCompass = define(builder, "waypoint_compass", true);
            enableSwordPedestal = define(builder, "sword_pedestal", true);
            enableFancyWorkbench = define(builder, "fancy_workbench", true);
            enableFancySign = define(builder, "fancy_sign", true);
            enableBigBook = define(builder, "big_book", true);
            enableSlottedBook = define(builder, "slotted_book", true);
            enableDeskBell = define(builder, "desk_bell", true);
            enableHandDrill = define(builder, "hand_drill", true);
            enableTypewriter = define(builder, "typewriter", true);
            enableClock = define(builder, "clock", true);
            enablePainting = define(builder, "painting", true);
            enableTesterItem = define(builder, "tester_item", true);
            enableAtlas = define(builder, "atlas", true);
            enableDeathCompass = define(builder, "death_compass", true);
            enableFurniturePaneler = define(builder, "furniture_paneler", true);
            enablePlumbLine = define(builder, "plumb_line", true);
            enableFramedChest = define(builder, "framed_chest", true);
            enableStockroomCatalog = define(builder, "stockroom_catalog", true);
            builder.pop();

            builder.push("recipes");
            enableLockRecipe = define(builder, "enable_lock_recipe", false);
            enableRecipeBookCrafting = define(builder, "enable_recipe_book_crafting", true);
            enablePublicTypesettingBooks = define(builder, "enable_public_typesetting_books", false);
            builder.pop();

            builder.push("rendering");
            forceFastRenderShelf = define(builder, "force_fast_render_shelf", false);
            forceFastRenderPotionShelf = define(builder, "force_fast_render_potion_shelf", true);
            forceFastRenderFancySign = define(builder, "force_fast_render_fancy_sign", true);
            forceFastRenderLabel = define(builder, "force_fast_render_label", true);
            forceFastRenderTabel = define(builder, "force_fast_render_table", false);
            forceFastRenderCase = define(builder, "force_fast_render_case", false);
            forceFastRenderToolrack = define(builder, "force_fast_render_toolrack", false);
            forceFastRenderDinnerPlate = define(builder, "force_fast_render_dinner_plate", false);
            forceFastRenderDiscRack = define(builder, "force_fast_render_disc_rack", false);
            emitLight = define(builder, "emit_light", true);
            disablerenderers = define(builder, "disable_renderers", false);
            renderDistancePainting = builder
                    .comment("Legacy setting: max render distance (in blocks) for paintings.")
                    .defineInRange("painting_render_distance", 64.0D, 1.0D, 512.0D);
            builder.pop();

            builder.push("gameplay");
            chairRedstone = define(builder, "chair_redstone", true);
            checkforupdate = define(builder, "check_for_update", true);
            enchPlateMaxUses = builder.defineInRange("enchanted_plate_max_uses", 3, 1, 64);
            enchantmentMultiplyer = builder.defineInRange("enchantment_cost_multiplier", 10, 1, 500);
            mapUpdateRate = builder.defineInRange("map_update_rate", 10, 1, 200);
            defaultBigBookTextScale = builder.defineInRange("default_big_book_text_scale", 0, 0, 7);
            builder.pop();

            builder.push("keywords");
            allowedBooks = builder.define(
                    "allowed_books",
                    "book, map, journal, plan, thaumonomicon, necronomicon, lexicon, print, notes, spell, library, tome, encyclopedia"
            );
            additionalTools = builder.define(
                    "additional_tools",
                    "sprayer, wand, rod, scepter, wrench, screwdriver, meter, handsaw, gun, cutter, scoop, soldering, painter, reader, shovel, grafter, pickaxe, pipette, magnifying, sword, axe, hammer"
            );
            additionalPotions = builder.define(
                    "additional_potions",
                    "essence, mead, bottle, test, element, molecule, can, capsule, cell, catalyst, ambrosia, honey pot, dissipation, vial, juice"
            );
            additionalDiscs = builder.define("additional_discs", "disc, disk");
            builder.pop();
        }

        private static ForgeConfigSpec.BooleanValue define(ForgeConfigSpec.Builder builder, String key, boolean defaultValue) {
            return builder.define(key, defaultValue);
        }
    }
}
