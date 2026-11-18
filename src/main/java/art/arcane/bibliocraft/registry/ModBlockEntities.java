package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.ArmorStandBlockEntity;
import art.arcane.bibliocraft.blockentity.BookcaseBlockEntity;
import art.arcane.bibliocraft.blockentity.CookieJarBlockEntity;
import art.arcane.bibliocraft.blockentity.DeskBellBlockEntity;
import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import art.arcane.bibliocraft.blockentity.DinnerPlateBlockEntity;
import art.arcane.bibliocraft.blockentity.DiscRackBlockEntity;
import art.arcane.bibliocraft.blockentity.SwordPedestalBlockEntity;
import art.arcane.bibliocraft.blockentity.TypewriterBlockEntity;
import art.arcane.bibliocraft.blockentity.DisplayCaseBlockEntity;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.blockentity.PlaceholderBlockEntity;
import art.arcane.bibliocraft.blockentity.FurniturePanelerBlockEntity;
import art.arcane.bibliocraft.blockentity.RackBlockEntity;
import art.arcane.bibliocraft.blockentity.ShelfBlockEntity;
import art.arcane.bibliocraft.blockentity.SeatBlockEntity;
import art.arcane.bibliocraft.blockentity.TableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BiblioCraft.MODID);

    public static final RegistryObject<BlockEntityType<BookcaseBlockEntity>> BOOKCASE =
            BLOCK_ENTITIES.register("bookcase", () -> BlockEntityType.Builder.of(
                    BookcaseBlockEntity::new, ModBlocks.BOOKCASE.get(), ModBlocks.BOOKCASE_CREATIVE.get()).build(null));
    public static final RegistryObject<BlockEntityType<ShelfBlockEntity>> SHELF =
            BLOCK_ENTITIES.register("shelf", () ->
                    BlockEntityType.Builder.of(ShelfBlockEntity::new, ModBlocks.SHELF.get()).build(null));
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> MARKER_POLE =
            register("marker_pole", ModBlocks.MARKER_POLE);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> CLIPBOARD =
            register("clipboard", ModBlocks.CLIPBOARD);
    public static final RegistryObject<BlockEntityType<FurnitureBlockEntity>> BIBLIOLIGHT =
            BLOCK_ENTITIES.register("bibliolight", () -> BlockEntityType.Builder.of(ModBlockEntities::createLight,
                    ModBlocks.LAMP_GOLD.get(), ModBlocks.LAMP_IRON.get(), ModBlocks.LANTERN_GOLD.get(),
                    ModBlocks.LANTERN_IRON.get()).build(null));
    public static final RegistryObject<BlockEntityType<FurniturePanelerBlockEntity>> FURNITURE_PANELER =
            BLOCK_ENTITIES.register("furniture_paneler", () ->
                    BlockEntityType.Builder.of(FurniturePanelerBlockEntity::new, ModBlocks.FURNITURE_PANELER.get()).build(null));
    public static final RegistryObject<BlockEntityType<RackBlockEntity>> POTION_SHELF =
            BLOCK_ENTITIES.register("potion_shelf", () ->
                    BlockEntityType.Builder.of(RackBlockEntity::potionShelf, ModBlocks.POTION_SHELF.get()).build(null));
    public static final RegistryObject<BlockEntityType<RackBlockEntity>> TOOL_RACK =
            BLOCK_ENTITIES.register("tool_rack", () ->
                    BlockEntityType.Builder.of(RackBlockEntity::toolRack, ModBlocks.TOOL_RACK.get()).build(null));
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> LABEL =
            register("label", ModBlocks.LABEL);
    public static final RegistryObject<BlockEntityType<DeskBlockEntity>> DESK =
            BLOCK_ENTITIES.register("desk", () ->
                    BlockEntityType.Builder.of(DeskBlockEntity::new, ModBlocks.DESK.get()).build(null));
    public static final RegistryObject<BlockEntityType<TableBlockEntity>> TABLE =
            BLOCK_ENTITIES.register("table", () -> BlockEntityType.Builder.of(
                    TableBlockEntity::new, ModBlocks.TABLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<SeatBlockEntity>> SEAT =
            BLOCK_ENTITIES.register("seat", () -> BlockEntityType.Builder.of(
                    SeatBlockEntity::new, ModBlocks.SEAT.get()).build(null));
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> FANCY_SIGN =
            register("fancy_sign", ModBlocks.FANCY_SIGN);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> FANCY_WORKBENCH =
            register("fancy_workbench", ModBlocks.FANCY_WORKBENCH);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> FRAMED_CHEST =
            register("framed_chest", ModBlocks.FRAMED_CHEST);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> MAP_FRAME =
            register("map_frame", ModBlocks.MAP_FRAME);
    public static final RegistryObject<BlockEntityType<DisplayCaseBlockEntity>> CASE =
            BLOCK_ENTITIES.register("case", () ->
                    BlockEntityType.Builder.of(DisplayCaseBlockEntity::new, ModBlocks.CASE.get()).build(null));
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> CLOCK =
            register("clock", ModBlocks.CLOCK);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> PAINTING_FRAME_BORDERLESS =
            register("painting_frame_borderless", ModBlocks.PAINTING_FRAME_BORDERLESS);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> PAINTING_FRAME_FANCY =
            register("painting_frame_fancy", ModBlocks.PAINTING_FRAME_FANCY);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> PAINTING_FRAME_FLAT =
            register("painting_frame_flat", ModBlocks.PAINTING_FRAME_FLAT);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> PAINTING_FRAME_MIDDLE =
            register("painting_frame_middle", ModBlocks.PAINTING_FRAME_MIDDLE);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> PAINTING_FRAME_SIMPLE =
            register("painting_frame_simple", ModBlocks.PAINTING_FRAME_SIMPLE);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> PAINTING_PRESS =
            register("painting_press", ModBlocks.PAINTING_PRESS);
    public static final RegistryObject<BlockEntityType<ArmorStandBlockEntity>> ARMOR_STAND =
            BLOCK_ENTITIES.register("armor_stand", () -> BlockEntityType.Builder.of(
                    ArmorStandBlockEntity::new, ModBlocks.ARMOR_STAND.get()).build(null));
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> TYPESETTING_TABLE =
            register("typesetting_table", ModBlocks.TYPESETTING_TABLE);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> PRINTING_PRESS =
            register("printing_press", ModBlocks.PRINTING_PRESS);
    public static final RegistryObject<BlockEntityType<CookieJarBlockEntity>> COOKIE_JAR =
            BLOCK_ENTITIES.register("cookie_jar", () ->
                    BlockEntityType.Builder.of(CookieJarBlockEntity::new, ModBlocks.COOKIE_JAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<DinnerPlateBlockEntity>> DINNER_PLATE =
            BLOCK_ENTITIES.register("dinner_plate", () ->
                    BlockEntityType.Builder.of(DinnerPlateBlockEntity::new, ModBlocks.DINNER_PLATE.get()).build(null));
    public static final RegistryObject<BlockEntityType<DiscRackBlockEntity>> DISC_RACK =
            BLOCK_ENTITIES.register("disc_rack", () ->
                    BlockEntityType.Builder.of(DiscRackBlockEntity::new, ModBlocks.DISC_RACK.get()).build(null));
    public static final RegistryObject<BlockEntityType<SwordPedestalBlockEntity>> SWORD_PEDESTAL =
            BLOCK_ENTITIES.register("sword_pedestal", () ->
                    BlockEntityType.Builder.of(SwordPedestalBlockEntity::new, ModBlocks.SWORD_PEDESTAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<DeskBellBlockEntity>> BELL =
            BLOCK_ENTITIES.register("bell", () ->
                    BlockEntityType.Builder.of(DeskBellBlockEntity::new, ModBlocks.BELL.get()).build(null));
    public static final RegistryObject<BlockEntityType<TypewriterBlockEntity>> TYPEWRITER =
            BLOCK_ENTITIES.register("typewriter", () ->
                    BlockEntityType.Builder.of(TypewriterBlockEntity::new, ModBlocks.TYPEWRITER.get()).build(null));

    private ModBlockEntities() {}

    private static FurnitureBlockEntity createLight(BlockPos pos, BlockState state) {
        return new FurnitureBlockEntity(BIBLIOLIGHT.get(), pos, state);
    }

    @SafeVarargs
    private static RegistryObject<BlockEntityType<PlaceholderBlockEntity>> register(
            String name,
            RegistryObject<Block>... validBlocks
    ) {
        @SuppressWarnings("unchecked")
        final RegistryObject<BlockEntityType<PlaceholderBlockEntity>>[] holder = new RegistryObject[1];

        holder[0] = BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder
                .of(
                        (pos, state) -> new PlaceholderBlockEntity(holder[0].get(), pos, state),
                        Arrays.stream(validBlocks).map(RegistryObject::get).toArray(Block[]::new)
                )
                .build(null));

        return holder[0];
    }
}
