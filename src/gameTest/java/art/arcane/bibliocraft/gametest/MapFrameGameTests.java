package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.block.MapFrameBlock;
import art.arcane.bibliocraft.blockentity.MapFrameBlockEntity;
import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.map.MapPin;
import art.arcane.bibliocraft.menu.MapFrameMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class MapFrameGameTests {
    private static final BlockPos PLACE = new BlockPos(2, 1, 2);
    private static final MapPin.Point TEST_POINT = new MapPin.Point(.125, .75);

    private MapFrameGameTests() {}

    @GameTest(template = "empty")
    public static void mapCoordinatesRemainStableAcrossAllMountsFacingsAndTurns(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(PLACE);
        for (AttachFace face : AttachFace.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                var state = ModBlocks.MAP_FRAME.get().defaultBlockState().setValue(MapFrameBlock.FACE, face)
                        .setValue(MapFrameBlock.FACING, facing);
                for (int rotation = 0; rotation < 4; rotation++) {
                    var drawn = TEST_POINT.rotate(rotation);
                    var surface = surface(face, drawn);
                    var hit = new BlockHitResult(rotate(facing, surface).add(Vec3.atLowerCornerOf(pos)),
                            MapFrameBlock.opening(state), pos, false);
                    helper.assertTrue(TEST_POINT.equals(MapFrameBlock.pointAt(state, hit, rotation)),
                            "A clicked pin must refer to one unrotated map pixel in every mount and rotation");
                }
                var wrongFace = new BlockHitResult(Vec3.atCenterOf(pos), MapFrameBlock.opening(state).getOpposite(),
                        pos, false);
                helper.assertTrue(MapFrameBlock.pointAt(state, wrongFace, 0) == null,
                        "The back of a map frame must not open the pin editor");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mapSeamsJoinAndDetachOnlyInTheSamePlane(GameTestHelper helper) {
        var wall = ModBlocks.MAP_FRAME.get().defaultBlockState().setValue(MapFrameBlock.FACE, AttachFace.WALL);
        helper.setBlock(PLACE, wall);
        helper.setBlock(PLACE.above(), wall);
        helper.setBlock(PLACE.north(), wall);
        helper.setBlock(PLACE.south(), wall.setValue(MapFrameBlock.FACE, AttachFace.FLOOR));
        helper.assertTrue(parts(helper, PLACE) == (511 & ~(2 | 4 | 64)),
                "The joining edges and the shared top-right corner should disappear");
        helper.setBlock(PLACE.above(), Blocks.AIR);
        helper.assertTrue(parts(helper, PLACE) == (511 & ~4), "Taking out the upper chart should restore its seam");
        var floor = wall.setValue(MapFrameBlock.FACE, AttachFace.FLOOR);
        helper.setBlock(PLACE, floor);
        helper.setBlock(PLACE.north(), floor.setValue(MapFrameBlock.FACING, Direction.SOUTH));
        helper.assertTrue(parts(helper, PLACE) == (511 & ~(2 | 8)),
                "Floor frames under either facing should still join both north and south");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void installedPinsNameTheirWorldTargetAndDropWithTheirMap(GameTestHelper helper) {
        var frame = frame(helper);
        var author = player(helper);
        var marker = new MapPin(UUID.randomUUID(), TEST_POINT, "Old bridge", DyeColor.CYAN);
        ItemStack map = MapItem.create(helper.getLevel(), 100, -60, (byte) 1, true, false);
        MapPin.write(map, List.of(marker));
        Integer mapId = MapItem.getMapId(map);
        var target = marker.target(MapItem.getSavedData(map, helper.getLevel()));
        var skin = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("bricks"));
        var tag = frame.saveWithFullMetadata();
        skin.save(tag);
        frame.load(tag);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(frame.getBlockPos()).add(surface(AttachFace.FLOOR, TEST_POINT)),
                Direction.UP, frame.getBlockPos(), false);
        author.setItemInHand(InteractionHand.MAIN_HAND, map.copyWithCount(2));
        helper.useBlock(PLACE, author, hit);
        helper.assertTrue(author.getMainHandItem().getCount() == 1 && frame.pins().equals(List.of(marker)),
                "A direct click must transfer one map with its pins");
        var compass = new ItemStack(ModItems.WAYPOINT_COMPASS.get());
        author.setItemInHand(InteractionHand.MAIN_HAND, compass);
        helper.useBlock(PLACE, author, hit);
        helper.assertTrue(WaypointCompassItem.getTarget(compass).equals(target),
                "A compass clicked against the pin must inherit its position and dimension");
        frame.rotateMap();
        var restored = (MapFrameBlockEntity) BlockEntity.loadStatic(frame.getBlockPos(), frame.getBlockState(),
                frame.saveWithFullMetadata());
        helper.assertTrue(restored.rotation() == 1 && restored.pins().equals(List.of(marker))
                        && restored.getMaterial().equals(skin),
                "A reload must preserve the facing-independent data, material, and display rotation");
        helper.getLevel().destroyBlock(frame.getBlockPos(), true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(frame.getBlockPos()).inflate(1));
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(ModBlocks.MAP_FRAME.get().asItem())
                        && FurnitureMaterial.fromItem(drop.getItem()).equals(skin))
                        && drops.stream().filter(drop -> drop.getItem().is(Items.FILLED_MAP)
                        && mapId.equals(MapItem.getMapId(drop.getItem())) && MapPin.read(drop.getItem()).equals(List.of(marker)))
                        .mapToInt(drop -> drop.getItem().getCount()).sum() == 1,
                "Harvest should return exactly one map with its original pins and a separate custom frame");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pinEditsRespectMapIdentityAndConcurrentChanges(GameTestHelper helper) {
        var frame = frame(helper);
        frame.setItem(0, MapItem.create(helper.getLevel(), 0, 0, (byte) 0, true, false));
        var first = player(helper);
        var second = player(helper);
        var pin = new MapPin(UUID.randomUUID(), TEST_POINT, "", DyeColor.RED);
        var edit = menu(1, frame, first, pin, false);
        helper.assertTrue(edit.save(first, "  River  ", DyeColor.LIME, false), "Save should add one colored pin");
        MapPin created = frame.pins().get(0);
        helper.assertTrue(created.name().equals("River") && created.color() == DyeColor.LIME
                        && !edit.save(first, "Duplicate", DyeColor.BLACK, false),
                "Replaying an add must not create another pin at the same spot");
        var firstEdit = menu(2, frame, first, created, true);
        var secondEdit = menu(3, frame, second, created, true);
        helper.assertTrue(firstEdit.save(first, "River mouth", DyeColor.BLUE, false)
                        && !secondEdit.save(second, "Older edit", DyeColor.RED, false),
                "A newer edit must win over a stale copy of the same pin");
        var later = menu(4, frame, second, frame.pins().get(0), true);
        frame.toggleLock(first);
        helper.assertTrue(!later.save(second, "", DyeColor.RED, true), "A guest must not remove a locked frame pin");
        frame.toggleLock(first);
        helper.assertTrue(later.save(second, "", DyeColor.RED, true) && frame.pins().isEmpty(),
                "A permitted removal should delete only the selected pin");
        frame.setItem(0, frame.getItem(0).copy());
        helper.assertTrue(!later.stillValid(second), "A reopened or replaced map must invalidate the old editor");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            created.write(buffer);
            helper.assertTrue(created.equals(MapPin.read(buffer)) && !buffer.isReadable(),
                    "A pin must round-trip through the map menu codec");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mapFrameRecipesProduceAllFurnitureMaterials(GameTestHelper helper) {
        var player = player(helper);
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            for (int index = 0; index < 9; index++) {
                var item = index != 4 ? Items.STICK : wood == FurnitureMaterial.Wood.FRAMED ? ModItems.FRAMING_BOARD.get()
                        : BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
                grid.setItem(index, new ItemStack(item));
            }
            String path = wood == FurnitureMaterial.Wood.OAK ? "map_frame" : "map_frame_" + wood.id();
            var recipe = (CraftingRecipe) helper.getLevel().getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, path)).orElseThrow();
            var output = recipe.assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(recipe.matches(grid, helper.getLevel()) && output.is(ModBlocks.MAP_FRAME.get().asItem())
                    && FurnitureMaterial.fromItem(output).wood() == wood, "The legacy recipe must preserve its wood");
        }
        helper.succeed();
    }

    private static Vec3 surface(AttachFace face, MapPin.Point point) {
        return switch (face) {
            case WALL -> new Vec3(.045, 1 - point.v(), 1 - point.u());
            case FLOOR -> new Vec3(point.u(), .045, point.v());
            case CEILING -> new Vec3(point.u(), .955, 1 - point.v());
        };
    }

    private static Vec3 rotate(Direction facing, Vec3 p) {
        return switch (facing) {
            case EAST -> new Vec3(1 - p.z, p.y, p.x);
            case SOUTH -> new Vec3(1 - p.x, p.y, 1 - p.z);
            case WEST -> new Vec3(p.z, p.y, 1 - p.x);
            default -> p;
        };
    }

    private static MapFrameMenu menu(int id, MapFrameBlockEntity frame, Player player, MapPin pin, boolean editing) {
        ItemStack tool = new ItemStack(ModItems.MAP_TOOL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var menu = new MapFrameMenu(id, frame, InteractionHand.MAIN_HAND, tool, new MapFrameMenu.View(pin, editing, 1, 0, 0));
        player.containerMenu = menu;
        return menu;
    }

    private static MapFrameBlockEntity frame(GameTestHelper helper) {
        helper.setBlock(PLACE, ModBlocks.MAP_FRAME.get());
        return (MapFrameBlockEntity) helper.getBlockEntity(PLACE);
    }

    private static int parts(GameTestHelper helper, BlockPos pos) {
        return MapFrameBlock.visibleParts(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos));
    }

    private static Player player(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(PLACE)).add(1, 0, 0));
        return player;
    }
}
