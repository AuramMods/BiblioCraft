package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.ArmorStandBlock;
import art.arcane.bibliocraft.block.BookcaseBlock;
import art.arcane.bibliocraft.block.ClipboardBlock;
import art.arcane.bibliocraft.block.ColorMountedFacingEntityBlock;
import art.arcane.bibliocraft.block.CookieJarBlock;
import art.arcane.bibliocraft.block.DeskBellBlock;
import art.arcane.bibliocraft.block.DeskBlock;
import art.arcane.bibliocraft.block.DinnerPlateBlock;
import art.arcane.bibliocraft.block.DiscRackBlock;
import art.arcane.bibliocraft.block.DisplayCaseBlock;
import art.arcane.bibliocraft.block.FurniturePanelerBlock;
import art.arcane.bibliocraft.block.FancySignBlock;
import art.arcane.bibliocraft.block.FramedChestBlock;
import art.arcane.bibliocraft.block.ClockBlock;
import art.arcane.bibliocraft.block.LabelBlock;
import art.arcane.bibliocraft.block.MapFrameBlock;
import art.arcane.bibliocraft.block.MarkerPoleBlock;
import art.arcane.bibliocraft.block.RackBlock;
import art.arcane.bibliocraft.block.RackType;
import art.arcane.bibliocraft.block.ShelfBlock;
import art.arcane.bibliocraft.block.SeatBlock;
import art.arcane.bibliocraft.block.SwordPedestalBlock;
import art.arcane.bibliocraft.block.TableBlock;
import art.arcane.bibliocraft.block.TypewriterBlock;
import art.arcane.bibliocraft.block.TypesettingTableBlock;
import art.arcane.bibliocraft.block.PrintingPressBlock;
import art.arcane.bibliocraft.block.PaintingFrameBlock;
import art.arcane.bibliocraft.block.PaintingPressBlock;
import art.arcane.bibliocraft.item.BiblioBlockItem;
import art.arcane.bibliocraft.item.ColoredBlockItem;
import art.arcane.bibliocraft.item.FurnitureBlockItem;
import art.arcane.bibliocraft.block.FancyWorkbenchBlock;
import art.arcane.bibliocraft.item.FancySignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
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
import java.util.function.Function;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BiblioCraft.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BiblioCraft.MODID);

    private static final List<RegistryObject<Item>> TAB_ITEMS = new ArrayList<>();

    private static final VoxelShape SHAPE_HALF_RACK = box(0.0, 0.0, 0.0, 0.5, 1.0, 1.0);
    private static final VoxelShape SHAPE_HALF_RACK_EAST = box(0.5, 0.0, 0.0, 1.0, 1.0, 1.0);
    private static final VoxelShape SHAPE_CLIPBOARD = box(0, 0.0725, 0.1525, 0.077, 0.9595, 0.8475);
    private static final VoxelShape SHAPE_LANTERN = box(0.2133, 0.0, 0.2517, 0.7867, 0.7007, 0.7483);
    private static final VoxelShape SHAPE_LANTERN_WALL = box(0.2133, 0.0, 0.2517, 1.0, 0.9789, 0.7483);
    private static final VoxelShape SHAPE_LANTERN_CEILING = box(0.15, 0.0, 0.1971, 0.85, 1.0, 0.8033);
    private static final VoxelShape SHAPE_LAMP = box(0.0485, 0.0, 0.1, 0.85, 1.0, 0.9);
    private static final VoxelShape SHAPE_LAMP_WALL = box(0.0485, 0.0, 0.1, 1.0, 0.5403, 0.9);
    private static final VoxelShape SHAPE_LAMP_CEILING = box(0.0485, 0.0, 0.1, 0.9515, 1.0, 0.9);
    private static final VoxelShape SHAPE_FURNITURE_PANELER = box(0.0, 0.0, 0.0, 1.0, 0.63, 1.0);
    private static final VoxelShape SHAPE_CASE_FLOOR = combine(
            box(0.0625, 0, 0, 0.9375, 0.5, 1),
            box(0.9375, 0.3125, 0.4375, 1, 0.4375, 0.5625)
    );
    private static final VoxelShape SHAPE_CASE_WALL = combine(
            box(0, 0, 0.0625, 0.5, 1, 0.9375),
            box(0.3125, 0.4375, 0, 0.4375, 0.5625, 0.0625)
    );
    private static final VoxelShape SHAPE_TYPEWRITER = box(0.1384, 0.0, 0.2466, 0.6452, 0.1966, 0.7534);
    private static final VoxelShape SHAPE_SWORD_PEDESTAL = box(0.2758, 0.0, 0.0712, 0.7242, 0.2340, 0.9288);
    private static final VoxelShape SHAPE_BELL = box(0.36518, 0, 0.36518, 0.63482, 0.2, 0.63482);
    private static final VoxelShape SHAPE_TYPESETTING_TABLE = box(0.0, 0.0, 0.0, 1.0, 0.92, 1.0);
    private static final VoxelShape SHAPE_COOKIE_JAR = box(0.18, 0.0, 0.18, 0.82, 0.75, 0.82);
    private static final VoxelShape SHAPE_DINNER_PLATE = box(0.1536, 0.0, 0.11, 0.8464, 0.0767, 0.91);
    private static final VoxelShape SHAPE_DISC_RACK = box(0.0, 0.0, 0.25, 1.0, 0.35, 0.75);

    public static final RegistryObject<Block> BOOKCASE =
            register("bookcase", () -> new BookcaseBlock(woodProps(), SHAPE_HALF_RACK, false),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> BOOKCASE_CREATIVE =
            register("bookcase_creative", () -> new BookcaseBlock(woodProps(), SHAPE_HALF_RACK, true),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> SHELF =
            register("shelf", () -> new ShelfBlock(woodProps(), SHAPE_HALF_RACK),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> MARKER_POLE =
            register("marker_pole", () -> new MarkerPoleBlock(woodProps().mapColor(MapColor.METAL).sound(SoundType.ANVIL)));
    public static final RegistryObject<Block> CLIPBOARD =
            register("clipboard", () -> new ClipboardBlock(woodProps().sound(SoundType.WOOD), SHAPE_CLIPBOARD));
    public static final RegistryObject<Block> LANTERN_GOLD =
            registerMountedLight("lantern_gold", SHAPE_LANTERN, SHAPE_LANTERN_WALL, SHAPE_LANTERN_CEILING, true);
    public static final RegistryObject<Block> LANTERN_IRON =
            registerMountedLight("lantern_iron", SHAPE_LANTERN, SHAPE_LANTERN_WALL, SHAPE_LANTERN_CEILING, true);
    public static final RegistryObject<Block> LAMP_GOLD =
            registerMountedLight("lamp_gold", SHAPE_LAMP, SHAPE_LAMP_WALL, SHAPE_LAMP_CEILING, false);
    public static final RegistryObject<Block> LAMP_IRON =
            registerMountedLight("lamp_iron", SHAPE_LAMP, SHAPE_LAMP_WALL, SHAPE_LAMP_CEILING, false);
    public static final RegistryObject<Block> FURNITURE_PANELER =
            register("furniture_paneler", () -> new FurniturePanelerBlock(woodProps(), SHAPE_FURNITURE_PANELER),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> FRAMED_CHEST =
            register("framed_chest", () -> new FramedChestBlock(woodProps().sound(SoundType.WOOD)),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> FANCY_SIGN =
            register("fancy_sign", () -> new FancySignBlock(woodProps().sound(SoundType.WOOD)),
                    block -> new FancySignItem(block, new Item.Properties()));
    public static final RegistryObject<Block> FANCY_WORKBENCH =
            register("fancy_workbench", () -> new FancyWorkbenchBlock(woodProps().sound(SoundType.WOOD)),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> POTION_SHELF =
            register("potion_shelf", () -> new RackBlock(woodProps(), SHAPE_HALF_RACK_EAST, "potion_shelf", RackType.POTIONS),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> TOOL_RACK =
            register("tool_rack", () -> new RackBlock(woodProps(), SHAPE_HALF_RACK, "tool_rack", RackType.TOOLS),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> LABEL =
            register("label", () -> new LabelBlock(woodProps().sound(SoundType.WOOD)),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> DESK =
            register("desk", () -> new DeskBlock(woodProps().sound(SoundType.WOOD)),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> TABLE =
            register("table", () -> new TableBlock(woodProps().sound(SoundType.WOOD).dynamicShape()),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> SEAT =
            register("seat", () -> new SeatBlock(woodProps().sound(SoundType.WOOD).dynamicShape()),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> CLOCK =
            register("clock", () -> new ClockBlock(woodProps().sound(SoundType.WOOD)),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> CASE =
            register("case", () -> new DisplayCaseBlock(woodProps(), SHAPE_CASE_FLOOR, SHAPE_CASE_WALL),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> MAP_FRAME =
            register("map_frame", () -> new MapFrameBlock(woodProps().sound(SoundType.WOOD)),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> PAINTING_FRAME_FLAT =
            paintingFrame("painting_frame_flat", PaintingFrameBlock.Style.FLAT);
    public static final RegistryObject<Block> PAINTING_FRAME_SIMPLE =
            paintingFrame("painting_frame_simple", PaintingFrameBlock.Style.SIMPLE);
    public static final RegistryObject<Block> PAINTING_FRAME_MIDDLE =
            paintingFrame("painting_frame_middle", PaintingFrameBlock.Style.MIDDLE);
    public static final RegistryObject<Block> PAINTING_FRAME_FANCY =
            paintingFrame("painting_frame_fancy", PaintingFrameBlock.Style.FANCY);
    public static final RegistryObject<Block> PAINTING_FRAME_BORDERLESS =
            paintingFrame("painting_frame_borderless", PaintingFrameBlock.Style.BORDERLESS);
    public static final RegistryObject<Block> PAINTING_PRESS =
            register("painting_press", () -> new PaintingPressBlock(woodProps().mapColor(MapColor.METAL)
                    .sound(SoundType.ANVIL)));
    public static final RegistryObject<Block> TYPEWRITER =
            register("typewriter", () -> new TypewriterBlock(woodProps().mapColor(MapColor.METAL)
                            .sound(SoundType.METAL), SHAPE_TYPEWRITER),
                    block -> new ColoredBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> SWORD_PEDESTAL =
            register("sword_pedestal", () -> new SwordPedestalBlock(
                            woodProps().mapColor(MapColor.STONE).sound(SoundType.STONE), SHAPE_SWORD_PEDESTAL),
                    block -> new ColoredBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> ARMOR_STAND =
            register("armor_stand", () -> new ArmorStandBlock(woodProps().sound(SoundType.WOOD)),
                    block -> new FurnitureBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> BELL =
            register("bell", () -> new DeskBellBlock(woodProps().mapColor(MapColor.METAL).sound(SoundType.METAL), SHAPE_BELL));
    public static final RegistryObject<Block> TYPESETTING_TABLE =
            register("typesetting_table", () -> new TypesettingTableBlock(woodProps().sound(SoundType.WOOD),
                    SHAPE_TYPESETTING_TABLE));
    public static final RegistryObject<Block> PRINTING_PRESS =
            register("printing_press", () -> new PrintingPressBlock(woodProps().mapColor(MapColor.METAL)
                    .sound(SoundType.ANVIL)));
    public static final RegistryObject<Block> COOKIE_JAR =
            register("cookie_jar", () -> new CookieJarBlock(woodProps(), SHAPE_COOKIE_JAR));
    public static final RegistryObject<Block> DINNER_PLATE =
            register("dinner_plate", () -> new DinnerPlateBlock(woodProps(), SHAPE_DINNER_PLATE));
    public static final RegistryObject<Block> DISC_RACK =
            register("disc_rack", () -> new DiscRackBlock(woodProps(), SHAPE_DISC_RACK));

    private ModBlocks() {}

    public static List<RegistryObject<Item>> getTabItems() {
        return Collections.unmodifiableList(TAB_ITEMS);
    }

    private static RegistryObject<Block> register(String name, Supplier<Block> blockSupplier) {
        return register(name, blockSupplier, block -> new BiblioBlockItem(block, new Item.Properties()));
    }

    private static RegistryObject<Block> register(String name, Supplier<Block> blockSupplier,
                                                  Function<Block, Item> itemFactory) {
        RegistryObject<Block> block = BLOCKS.register(name, blockSupplier);
        RegistryObject<Item> blockItem = ITEMS.register(name, () -> itemFactory.apply(block.get()));
        TAB_ITEMS.add(blockItem);
        return block;
    }

    private static RegistryObject<Block> registerMountedLight(String name, VoxelShape floorShape,
                                                              VoxelShape wallShape, VoxelShape ceilingShape, boolean candle) {
        return register(name, () -> new ColorMountedFacingEntityBlock(lightProps(), floorShape, wallShape, ceilingShape, candle),
                block -> new ColoredBlockItem(block, new Item.Properties()));
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
        return woodProps().mapColor(MapColor.METAL).sound(SoundType.METAL).lightLevel(state -> 15);
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
    private static RegistryObject<Block> paintingFrame(String path, PaintingFrameBlock.Style style) {
        return register(path, () -> new PaintingFrameBlock(woodProps().sound(SoundType.WOOD).dynamicShape(), style, path),
                block -> new FurnitureBlockItem(block, new Item.Properties()));
    }
}
