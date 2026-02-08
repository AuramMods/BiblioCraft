package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BiblioCraft.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BiblioCraft.MODID);

    private static final List<RegistryObject<Item>> TAB_ITEMS = new ArrayList<>();

    public static final RegistryObject<Block> BOOKCASE = register("bookcase", () -> new Block(woodProps()));
    public static final RegistryObject<Block> BOOKCASE_CREATIVE = register("bookcase_creative", () -> new Block(woodProps()));
    public static final RegistryObject<Block> SHELF = register("shelf", () -> new Block(woodProps()));
    public static final RegistryObject<Block> MARKER_POLE = register("marker_pole", () -> new Block(woodProps()));
    public static final RegistryObject<Block> CLIPBOARD = register("clipboard", () -> new Block(woodProps()));
    public static final RegistryObject<Block> LANTERN_GOLD = register("lantern_gold", () -> new Block(lightProps()));
    public static final RegistryObject<Block> LANTERN_IRON = register("lantern_iron", () -> new Block(lightProps()));
    public static final RegistryObject<Block> LAMP_GOLD = register("lamp_gold", () -> new Block(lightProps()));
    public static final RegistryObject<Block> LAMP_IRON = register("lamp_iron", () -> new Block(lightProps()));
    public static final RegistryObject<Block> FURNITURE_PANELER = register("furniture_paneler", () -> new Block(woodProps()));
    public static final RegistryObject<Block> FRAMED_CHEST = register("framed_chest", () -> new Block(woodProps()));
    public static final RegistryObject<Block> FANCY_SIGN = register("fancy_sign", () -> new Block(woodProps()));
    public static final RegistryObject<Block> FANCY_WORKBENCH = register("fancy_workbench", () -> new Block(woodProps()));
    public static final RegistryObject<Block> POTION_SHELF = register("potion_shelf", () -> new Block(woodProps()));
    public static final RegistryObject<Block> TOOL_RACK = register("tool_rack", () -> new Block(woodProps()));
    public static final RegistryObject<Block> LABEL = register("label", () -> new Block(woodProps()));
    public static final RegistryObject<Block> DESK = register("desk", () -> new Block(woodProps()));
    public static final RegistryObject<Block> TABLE = register("table", () -> new Block(woodProps()));
    public static final RegistryObject<Block> SEAT = register("seat", () -> new Block(woodProps()));
    public static final RegistryObject<Block> CLOCK = register("clock", () -> new Block(woodProps()));
    public static final RegistryObject<Block> CASE = register("case", () -> new Block(woodProps()));
    public static final RegistryObject<Block> MAP_FRAME = register("map_frame", () -> new Block(woodProps()));
    public static final RegistryObject<Block> PAINTING_FRAME_FLAT = register("painting_frame_flat", () -> new Block(woodProps()));
    public static final RegistryObject<Block> PAINTING_FRAME_SIMPLE = register("painting_frame_simple", () -> new Block(woodProps()));
    public static final RegistryObject<Block> PAINTING_FRAME_MIDDLE = register("painting_frame_middle", () -> new Block(woodProps()));
    public static final RegistryObject<Block> PAINTING_FRAME_FANCY = register("painting_frame_fancy", () -> new Block(woodProps()));
    public static final RegistryObject<Block> PAINTING_FRAME_BORDERLESS = register("painting_frame_borderless", () -> new Block(woodProps()));
    public static final RegistryObject<Block> PAINTING_PRESS = register("painting_press", () -> new Block(woodProps()));
    public static final RegistryObject<Block> TYPEWRITER = register("typewriter", () -> new Block(woodProps()));
    public static final RegistryObject<Block> SWORD_PEDESTAL = register("sword_pedestal", () -> new Block(woodProps()));
    public static final RegistryObject<Block> ARMOR_STAND = register("armor_stand", () -> new Block(woodProps()));
    public static final RegistryObject<Block> BELL = register("bell", () -> new Block(woodProps()));
    public static final RegistryObject<Block> TYPESETTING_TABLE = register("typesetting_table", () -> new Block(woodProps()));
    public static final RegistryObject<Block> PRINTING_PRESS = register("printing_press", () -> new Block(woodProps()));
    public static final RegistryObject<Block> COOKIE_JAR = register("cookie_jar", () -> new Block(woodProps()));
    public static final RegistryObject<Block> DINNER_PLATE = register("dinner_plate", () -> new Block(woodProps()));
    public static final RegistryObject<Block> DISC_RACK = register("disc_rack", () -> new Block(woodProps()));

    private ModBlocks() {}

    public static List<RegistryObject<Item>> getTabItems() {
        return Collections.unmodifiableList(TAB_ITEMS);
    }

    private static RegistryObject<Block> register(String name, Supplier<Block> blockSupplier) {
        RegistryObject<Block> block = BLOCKS.register(name, blockSupplier);
        RegistryObject<Item> blockItem = ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        TAB_ITEMS.add(blockItem);
        return block;
    }

    private static BlockBehaviour.Properties woodProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F);
    }

    private static BlockBehaviour.Properties lightProps() {
        return woodProps().lightLevel(state -> 15);
    }
}
