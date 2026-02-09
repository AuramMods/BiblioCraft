package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.HorizontalFacingEntityBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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

    private static final VoxelShape SHAPE_BOOKCASE = box(0.0, 0.0, 0.0, 0.5, 1.0, 1.0);
    private static final VoxelShape SHAPE_HALF_RACK = box(0.0, 0.0, 0.0, 0.5, 1.0, 1.0);
    private static final VoxelShape SHAPE_HALF_RACK_EAST = box(0.5, 0.0, 0.0, 1.0, 1.0, 1.0);
    private static final VoxelShape SHAPE_MARKER_POLE = box(0.25, 0.0, 0.25, 0.75, 0.9, 0.75);
    private static final VoxelShape SHAPE_CLIPBOARD = box(0.0, 0.08, 0.15, 0.08, 0.92, 0.85);
    private static final VoxelShape SHAPE_LANTERN = box(0.3, 0.0, 0.3, 0.7, 0.7, 0.7);
    private static final VoxelShape SHAPE_LAMP = box(0.18, 0.0, 0.18, 0.82, 1.0, 0.82);
    private static final VoxelShape SHAPE_FURNITURE_PANELER = box(0.0, 0.0, 0.0, 1.0, 0.63, 1.0);
    private static final VoxelShape SHAPE_FRAMED_CHEST = box(0.054, 0.0, 0.054, 0.946, 0.866, 0.946);
    private static final VoxelShape SHAPE_FANCY_SIGN = box(0.0, 0.2, 0.0, 0.06, 0.8, 1.0);
    private static final VoxelShape SHAPE_LABEL = box(0.0, 0.12, 0.28, 0.06, 0.38, 0.72);
    private static final VoxelShape SHAPE_TABLE = combine(
            box(0.0, 0.88, 0.0, 1.0, 1.0, 1.0),
            box(0.375, 0.0, 0.375, 0.625, 0.88, 0.625)
    );
    private static final VoxelShape SHAPE_SEAT = box(0.16, 0.0, 0.16, 0.84, 0.74, 0.84);
    private static final VoxelShape SHAPE_CLOCK = box(0.3, 0.0, 0.3, 0.7, 1.0, 0.7);
    private static final VoxelShape SHAPE_CASE = box(0.06, 0.0, 0.06, 0.94, 0.5, 0.94);
    private static final VoxelShape SHAPE_MAP_FRAME = box(0.0, 0.0, 0.0, 0.08, 1.0, 1.0);
    private static final VoxelShape SHAPE_PAINTING_FRAME = box(0.0, 0.0, 0.0, 0.08, 1.0, 1.0);
    private static final VoxelShape SHAPE_PAINTING_PRESS = box(0.0, 0.0, 0.0, 1.0, 0.98, 1.0);
    private static final VoxelShape SHAPE_TYPEWRITER = box(0.1384, 0.0, 0.2073, 0.7053, 0.4835, 0.7927);
    private static final VoxelShape SHAPE_SWORD_PEDESTAL = box(0.2758, 0.0, 0.0712, 0.7242, 0.2340, 0.9288);
    private static final VoxelShape SHAPE_ARMOR_STAND = box(0.3, 0.0, 0.3, 0.7, 1.0, 0.7);
    private static final VoxelShape SHAPE_BELL = box(0.4, 0.0, 0.4, 0.6, 0.2, 0.6);
    private static final VoxelShape SHAPE_TYPESETTING_TABLE = box(0.0, 0.0, 0.0, 1.0, 0.92, 1.0);
    private static final VoxelShape SHAPE_COOKIE_JAR = box(0.18, 0.0, 0.18, 0.82, 0.75, 0.82);
    private static final VoxelShape SHAPE_DINNER_PLATE = box(0.15, 0.0, 0.15, 0.85, 0.1, 0.85);
    private static final VoxelShape SHAPE_DISC_RACK = box(0.0, 0.0, 0.25, 1.0, 0.35, 0.75);

    public static final RegistryObject<Block> BOOKCASE =
            register("bookcase", () -> shapedWoodEntity(SHAPE_BOOKCASE, "bookcase"));
    public static final RegistryObject<Block> BOOKCASE_CREATIVE =
            register("bookcase_creative", () -> shapedWoodEntity(SHAPE_BOOKCASE, "bookcase"));
    public static final RegistryObject<Block> SHELF =
            register("shelf", () -> shapedWoodEntity(SHAPE_HALF_RACK, "shelf"));
    public static final RegistryObject<Block> MARKER_POLE =
            register("marker_pole", () -> shapedWoodEntity(SHAPE_MARKER_POLE, "marker_pole"));
    public static final RegistryObject<Block> CLIPBOARD =
            register("clipboard", () -> shapedWoodEntity(SHAPE_CLIPBOARD, "clipboard"));
    public static final RegistryObject<Block> LANTERN_GOLD =
            register("lantern_gold", () -> shapedLightEntity(SHAPE_LANTERN, "bibliolight"));
    public static final RegistryObject<Block> LANTERN_IRON =
            register("lantern_iron", () -> shapedLightEntity(SHAPE_LANTERN, "bibliolight"));
    public static final RegistryObject<Block> LAMP_GOLD =
            register("lamp_gold", () -> shapedLightEntity(SHAPE_LAMP, "bibliolight"));
    public static final RegistryObject<Block> LAMP_IRON =
            register("lamp_iron", () -> shapedLightEntity(SHAPE_LAMP, "bibliolight"));
    public static final RegistryObject<Block> FURNITURE_PANELER =
            register("furniture_paneler", () -> shapedWoodEntity(SHAPE_FURNITURE_PANELER, "furniture_paneler"));
    public static final RegistryObject<Block> FRAMED_CHEST =
            register("framed_chest", () -> shapedWoodEntity(SHAPE_FRAMED_CHEST, "framed_chest"));
    public static final RegistryObject<Block> FANCY_SIGN =
            register("fancy_sign", () -> shapedWoodEntity(SHAPE_FANCY_SIGN, "fancy_sign"));
    public static final RegistryObject<Block> FANCY_WORKBENCH =
            register("fancy_workbench", () -> fullWoodEntity("fancy_workbench"));
    public static final RegistryObject<Block> POTION_SHELF =
            register("potion_shelf", () -> shapedWoodEntity(SHAPE_HALF_RACK_EAST, "potion_shelf"));
    public static final RegistryObject<Block> TOOL_RACK =
            register("tool_rack", () -> shapedWoodEntity(SHAPE_HALF_RACK, "tool_rack"));
    public static final RegistryObject<Block> LABEL =
            register("label", () -> shapedWoodEntity(SHAPE_LABEL, "label"));
    public static final RegistryObject<Block> DESK =
            register("desk", () -> fullWoodEntity("desk"));
    public static final RegistryObject<Block> TABLE =
            register("table", () -> shapedWoodEntity(SHAPE_TABLE, "table"));
    public static final RegistryObject<Block> SEAT =
            register("seat", () -> shapedWoodEntity(SHAPE_SEAT, "seat"));
    public static final RegistryObject<Block> CLOCK =
            register("clock", () -> shapedWoodEntity(SHAPE_CLOCK, "clock"));
    public static final RegistryObject<Block> CASE =
            register("case", () -> shapedWoodEntity(SHAPE_CASE, "case"));
    public static final RegistryObject<Block> MAP_FRAME =
            register("map_frame", () -> shapedWoodEntity(SHAPE_MAP_FRAME, "map_frame"));
    public static final RegistryObject<Block> PAINTING_FRAME_FLAT =
            register("painting_frame_flat", () -> shapedWoodEntity(SHAPE_PAINTING_FRAME, "painting_frame_flat"));
    public static final RegistryObject<Block> PAINTING_FRAME_SIMPLE =
            register("painting_frame_simple", () -> shapedWoodEntity(SHAPE_PAINTING_FRAME, "painting_frame_simple"));
    public static final RegistryObject<Block> PAINTING_FRAME_MIDDLE =
            register("painting_frame_middle", () -> shapedWoodEntity(SHAPE_PAINTING_FRAME, "painting_frame_middle"));
    public static final RegistryObject<Block> PAINTING_FRAME_FANCY =
            register("painting_frame_fancy", () -> shapedWoodEntity(SHAPE_PAINTING_FRAME, "painting_frame_fancy"));
    public static final RegistryObject<Block> PAINTING_FRAME_BORDERLESS =
            register("painting_frame_borderless", () -> shapedWoodEntity(SHAPE_PAINTING_FRAME, "painting_frame_borderless"));
    public static final RegistryObject<Block> PAINTING_PRESS =
            register("painting_press", () -> shapedWoodEntity(SHAPE_PAINTING_PRESS, "painting_press"));
    public static final RegistryObject<Block> TYPEWRITER =
            register("typewriter", () -> shapedWoodEntity(SHAPE_TYPEWRITER, "typewriter"));
    public static final RegistryObject<Block> SWORD_PEDESTAL =
            register("sword_pedestal", () -> shapedWoodEntity(SHAPE_SWORD_PEDESTAL, "sword_pedestal"));
    public static final RegistryObject<Block> ARMOR_STAND =
            register("armor_stand", () -> shapedWoodEntity(SHAPE_ARMOR_STAND, "armor_stand"));
    public static final RegistryObject<Block> BELL =
            register("bell", () -> shapedWoodEntity(SHAPE_BELL, "bell"));
    public static final RegistryObject<Block> TYPESETTING_TABLE =
            register("typesetting_table", () -> shapedWoodEntity(SHAPE_TYPESETTING_TABLE, "typesetting_table"));
    public static final RegistryObject<Block> PRINTING_PRESS =
            register("printing_press", () -> fullWoodEntity("printing_press"));
    public static final RegistryObject<Block> COOKIE_JAR =
            register("cookie_jar", () -> shapedWoodEntity(SHAPE_COOKIE_JAR, "cookie_jar"));
    public static final RegistryObject<Block> DINNER_PLATE =
            register("dinner_plate", () -> shapedWoodEntity(SHAPE_DINNER_PLATE, "dinner_plate"));
    public static final RegistryObject<Block> DISC_RACK =
            register("disc_rack", () -> shapedWoodEntity(SHAPE_DISC_RACK, "disc_rack"));

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

    private static Block shapedWoodEntity(VoxelShape shape, String blockEntityPath) {
        return new HorizontalFacingEntityBlock(woodProps(), shape, blockEntityPath);
    }

    private static Block shapedLightEntity(VoxelShape shape, String blockEntityPath) {
        return new HorizontalFacingEntityBlock(lightProps(), shape, blockEntityPath);
    }

    private static Block fullWoodEntity(String blockEntityPath) {
        return new HorizontalFacingEntityBlock(woodProps(), Shapes.block(), blockEntityPath);
    }

    private static BlockBehaviour.Properties woodProps() {
        // OBJ display blocks should not occlude neighboring faces like full cubes.
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0F, 3.0F)
                .noOcclusion()
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .isRedstoneConductor((state, level, pos) -> false);
    }

    private static BlockBehaviour.Properties lightProps() {
        return woodProps().lightLevel(state -> 15);
    }

    private static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.box(x1 * 16.0, y1 * 16.0, z1 * 16.0, x2 * 16.0, y2 * 16.0, z2 * 16.0);
    }

    private static VoxelShape combine(VoxelShape... shapes) {
        VoxelShape output = Shapes.empty();
        for (VoxelShape shape : shapes) {
            output = Shapes.or(output, shape);
        }
        return output;
    }
}
