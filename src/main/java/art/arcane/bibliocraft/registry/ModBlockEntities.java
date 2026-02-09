package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.PlaceholderBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BiblioCraft.MODID);

    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> BOOKCASE =
            register("bookcase", ModBlocks.BOOKCASE, ModBlocks.BOOKCASE_CREATIVE);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> SHELF =
            register("shelf", ModBlocks.SHELF);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> MARKER_POLE =
            register("marker_pole", ModBlocks.MARKER_POLE);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> CLIPBOARD =
            register("clipboard", ModBlocks.CLIPBOARD);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> BIBLIOLIGHT =
            register("bibliolight", ModBlocks.LAMP_GOLD, ModBlocks.LAMP_IRON, ModBlocks.LANTERN_GOLD, ModBlocks.LANTERN_IRON);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> FURNITURE_PANELER =
            register("furniture_paneler", ModBlocks.FURNITURE_PANELER);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> POTION_SHELF =
            register("potion_shelf", ModBlocks.POTION_SHELF);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> TOOL_RACK =
            register("tool_rack", ModBlocks.TOOL_RACK);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> LABEL =
            register("label", ModBlocks.LABEL);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> DESK =
            register("desk", ModBlocks.DESK);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> TABLE =
            register("table", ModBlocks.TABLE);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> SEAT =
            register("seat", ModBlocks.SEAT);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> FANCY_SIGN =
            register("fancy_sign", ModBlocks.FANCY_SIGN);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> FANCY_WORKBENCH =
            register("fancy_workbench", ModBlocks.FANCY_WORKBENCH);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> FRAMED_CHEST =
            register("framed_chest", ModBlocks.FRAMED_CHEST);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> MAP_FRAME =
            register("map_frame", ModBlocks.MAP_FRAME);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> CASE =
            register("case", ModBlocks.CASE);
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
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> ARMOR_STAND =
            register("armor_stand", ModBlocks.ARMOR_STAND);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> TYPESETTING_TABLE =
            register("typesetting_table", ModBlocks.TYPESETTING_TABLE);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> PRINTING_PRESS =
            register("printing_press", ModBlocks.PRINTING_PRESS);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> COOKIE_JAR =
            register("cookie_jar", ModBlocks.COOKIE_JAR);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> DINNER_PLATE =
            register("dinner_plate", ModBlocks.DINNER_PLATE);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> DISC_RACK =
            register("disc_rack", ModBlocks.DISC_RACK);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> SWORD_PEDESTAL =
            register("sword_pedestal", ModBlocks.SWORD_PEDESTAL);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> BELL =
            register("bell", ModBlocks.BELL);
    public static final RegistryObject<BlockEntityType<PlaceholderBlockEntity>> TYPEWRITER =
            register("typewriter", ModBlocks.TYPEWRITER);

    private ModBlockEntities() {}

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
