package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.MapFrameBlockEntity;
import art.arcane.bibliocraft.map.AtlasContents;
import art.arcane.bibliocraft.map.MapPin;
import art.arcane.bibliocraft.menu.AtlasMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class AtlasGameTests {
    private static final MapPin.Point POINT = new MapPin.Point(.23, .72);

    private AtlasGameTests() {}

    @GameTest(template = "empty")
    public static void inventoryKeepsMapsSuppliesAndTheHeldAtlas(GameTestHelper helper) {
        Player player = player(helper);
        var held = player.getMainHandItem();
        var menu = new AtlasMenu(1, player.getInventory(), InteractionHand.MAIN_HAND, null);
        player.containerMenu = menu;
        var map = MapItem.create(helper.getLevel(), 0, 0, (byte) 0, true, false);
        player.getInventory().setItem(9, map.copyWithCount(2));
        player.getInventory().setItem(10, new ItemStack(Items.MAP, 8));
        player.getInventory().setItem(11, new ItemStack(Items.DIAMOND));
        menu.quickMoveStack(player, AtlasContents.SIZE);
        menu.quickMoveStack(player, AtlasContents.SIZE + 1);
        helper.assertTrue(menu.quickMoveStack(player, AtlasContents.SIZE + 2).isEmpty(), "Reject unrelated cargo");
        menu.clicked(6, player.getInventory().selected, ClickType.SWAP, player);
        helper.assertTrue(player.getMainHandItem() == held, "Number keys must not swap out the open atlas");
        ItemStack reloaded = ItemStack.of(held.save(new CompoundTag()));
        var contents = new AtlasContents(reloaded);
        helper.assertTrue(contents.getItem(0).getCount() == 8 && contents.getItem(6).getCount() == 1
                        && AtlasContents.sameMap(map, contents.selectedMap()) && player.getInventory().getItem(9).getCount() == 1,
                "Stacked sources must transfer at most one filled map per cell and preserve all supplies");
        menu.quickMoveStack(player, 6);
        helper.assertTrue(new AtlasContents(held).getItem(6).isEmpty()
                        && player.getInventory().countItem(Items.FILLED_MAP) == 2,
                "Removing a map returns it to the player and clears its saved slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void autoCenterCreatesOnlyAfterCrossingTheCurrentMap(GameTestHelper helper) {
        Player player = player(helper);
        var contents = new AtlasContents(player.getMainHandItem());
        contents.setOptions(true, true, 0);
        contents.setItem(0, new ItemStack(Items.MAP, 2));
        player.setPos(0, 80, 0);
        helper.assertTrue(contents.updateSelection(player, true) && contents.selected() == 6,
                "An empty atlas must create a map when the automatic settings are on");
        var first = contents.mapData(player.level(), 6);
        player.setPos(first.centerX + 63.99, 80, first.centerZ);
        helper.assertTrue(!contents.updateSelection(player, true) && contents.getItem(0).getCount() == 1,
                "Movement within the selected sheet must not consume another blank");
        player.setPos(first.centerX + 64, 80, first.centerZ);
        helper.assertTrue(contents.updateSelection(player, true) && contents.selected() == 7 && contents.getItem(0).isEmpty()
                        && contents.neighbor(player.level(), first, Direction.EAST) == 7,
                "Crossing the east seam should create an adjacent sheet with the same scale");
        player.setPos(first.centerX, 80, first.centerZ);
        helper.assertTrue(contents.updateSelection(player, true) && contents.selected() == 6,
                "Returning to known terrain should restore its original map without consuming supplies");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mapCopiesConsumeExactlyOneBlankAndPreservePins(GameTestHelper helper) {
        var atlas = new ItemStack(ModItems.ATLAS_BOOK.get());
        var contents = new AtlasContents(atlas);
        contents.setItem(0, new ItemStack(Items.MAP, 2));
        var map = MapItem.create(helper.getLevel(), 500, -404, (byte) 1, true, false);
        var pin = new MapPin(UUID.randomUUID(), POINT, "Old bridge", DyeColor.CYAN);
        MapPin.write(map, List.of(pin));
        helper.assertTrue(contents.copyOut().isEmpty() && contents.getItem(0).getCount() == 2,
                "Export without a selected map cannot consume paper");
        contents.setItem(8, new ItemStack(Items.MAP));
        helper.assertTrue(contents.copyIn(map) && contents.selected() == 8 && contents.getItem(0).getCount() == 2,
                "Import first fills a blank map already filed in the grid");
        helper.assertTrue(!contents.copyIn(map) && contents.getItem(0).getCount() == 2,
                "Trying to import the same map twice cannot consume another blank");
        var copy = contents.copyOut();
        helper.assertTrue(copy != contents.selectedMap() && AtlasContents.sameMap(map, copy)
                        && MapPin.read(copy).equals(List.of(pin)) && contents.getItem(0).getCount() == 1,
                "Export returns a new map stack with the same map ID and pins");
        MapPin.write(copy, List.of());
        helper.assertTrue(MapPin.read(new AtlasContents(atlas).selectedMap()).equals(List.of(pin)),
                "The copies can subsequently edit their own pins independently");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pinEditorBindsTheMapAndCompassToTheActiveAtlas(GameTestHelper helper) {
        Player player = player(helper);
        var original = player.getMainHandItem();
        var saved = new AtlasContents(original);
        var map = MapItem.create(helper.getLevel(), 960, -960, (byte) 1, true, false);
        int id = MapItem.getMapId(map);
        saved.setItem(0, new ItemStack(ModItems.WAYPOINT_COMPASS.get()));
        saved.setItem(6, map);
        saved.select(6);
        saved.opensMap(true);
        saved.saveInventory();
        var menu = new AtlasMenu(1, player.getInventory(), InteractionHand.MAIN_HAND, null);
        player.containerMenu = menu;
        helper.assertTrue(!menu.edit(player, AtlasMenu.Edit.ADD, 6, id + 1, null, POINT, "Ghost", DyeColor.RED, -1)
                        && menu.edit(player, AtlasMenu.Edit.ADD, 6, id, null, POINT, "Overlook", DyeColor.CYAN, -1)
                        && !menu.edit(player, AtlasMenu.Edit.ADD, 6, id, null, POINT, "Duplicate", DyeColor.RED, -1),
                "Pin edits must address the selected map and reject duplicates");
        var pin = MapPin.read(menu.selectedMap()).get(0);
        helper.assertTrue(menu.edit(player, AtlasMenu.Edit.TARGET, 6, id, pin.id(), POINT, "", DyeColor.RED, 0)
                        && pin.target(MapItem.getSavedData(map, player.level())).equals(AtlasContents.displayedCompass(original)),
                "Choosing an atlas compass must preserve the selected pin's coordinates and dimension");
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());
        helper.assertTrue(!menu.edit(player, AtlasMenu.Edit.REMOVE, 6, id, pin.id(), POINT, "", DyeColor.RED, -1)
                        && MapPin.read(menu.selectedMap()).size() == 1,
                "A delayed edit must not apply to a replacement atlas");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void frameTransferClosesWhenTheFrameOrMapChanges(GameTestHelper helper) {
        var at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ModBlocks.MAP_FRAME.get());
        var frame = (MapFrameBlockEntity) helper.getBlockEntity(at);
        Player visitor = player(helper);
        visitor.setPos(Vec3.atCenterOf(helper.absolutePos(at)).add(1, 0, 0));
        var map = MapItem.create(helper.getLevel(), 5, 15, (byte) 0, true, false);
        frame.setItem(0, map);
        var atlas = new AtlasContents(visitor.getMainHandItem());
        atlas.setItem(6, map.copy());
        atlas.select(6);
        atlas.saveInventory();
        var menu = new AtlasMenu(1, visitor.getInventory(), InteractionHand.MAIN_HAND, frame);
        helper.assertTrue(menu.mode() == AtlasMenu.Mode.TRANSFER && menu.stillValid(visitor),
                "Only the nearby matching filled map opens its transfer menu");
        Player owner = player(helper);
        frame.toggleLock(owner);
        helper.assertTrue(!menu.stillValid(visitor), "A newly locked frame must invalidate the guest transfer");
        frame.toggleLock(owner);
        frame.setItem(0, map.copy());
        helper.assertTrue(!menu.stillValid(visitor), "A replaced map stack must invalidate the prior transfer");
        helper.succeed();
    }

    private static Player player(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ATLAS_BOOK.get()));
        return player;
    }
}
