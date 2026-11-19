package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.FramedChestBlockEntity;
import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.item.StockroomCatalogItem;
import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.menu.StockroomCatalogMenu;
import art.arcane.bibliocraft.menu.WaypointCompassMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class StockroomCatalogGameTests {
    private static final BlockPos LEFT = new BlockPos(1, 1, 1);
    private static final BlockPos RIGHT = LEFT.east();
    private static final BlockPos FRAMED = LEFT.south();

    private StockroomCatalogGameTests() {}

    @GameTest(template = "empty")
    public static void catalogCountsBothPhysicalHalvesAndOnlyAccessibleFurniture(GameTestHelper helper) {
        var chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
        helper.setBlock(LEFT, chest.setValue(ChestBlock.TYPE, ChestType.LEFT));
        helper.setBlock(RIGHT, chest.setValue(ChestBlock.TYPE, ChestType.RIGHT));
        helper.setBlock(FRAMED, ModBlocks.FRAMED_CHEST.get());
        var left = (ChestBlockEntity) helper.getBlockEntity(LEFT);
        var right = (ChestBlockEntity) helper.getBlockEntity(RIGHT);
        var framed = (FramedChestBlockEntity) helper.getBlockEntity(FRAMED);
        left.setCustomName(Component.literal("First Stockroom"));
        left.setItem(0, new ItemStack(Items.IRON_INGOT, 13));
        left.setItem(1, new ItemStack(Items.IRON_INGOT, 4));
        right.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
        right.setItem(1, new ItemStack(Items.DIAMOND, 2));
        framed.setItem(0, new ItemStack(Items.DIAMOND, 5));

        Player owner = helper.makeMockSurvivalPlayer();
        Player visitor = helper.makeMockSurvivalPlayer();
        var item = (StockroomCatalogItem) ModItems.STOCKROOM_CATALOG.get();
        var catalog = new ItemStack(item);
        owner.setItemInHand(InteractionHand.OFF_HAND, catalog);
        owner.setShiftKeyDown(true);
        for (BlockPos pos : new BlockPos[]{left.getBlockPos(), right.getBlockPos(), framed.getBlockPos()}) {
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            helper.assertTrue(item.onItemUseFirst(catalog, new UseOnContext(owner, InteractionHand.OFF_HAND, hit))
                    .consumesAction(), "Crouch-use should register the clicked inventory to the held book");
        }
        var restored = ItemStack.of(catalog.save(new net.minecraft.nbt.CompoundTag()));
        helper.assertTrue(CatalogContents.locations(restored, helper.getLevel().dimension()).size() == 3,
                "Registered coordinates should survive saving the catalog");
        framed.toggleLock(owner);
        var ownStock = CatalogContents.scan(helper.getLevel(), owner, restored);
        var guestStock = CatalogContents.scan(helper.getLevel(), visitor, restored);
        var iron = entry(ownStock, Items.IRON_INGOT.getDescriptionId());
        helper.assertTrue(iron.count() == 49 && iron.details().size() == 2 && iron.details().get(0).count() == 32
                        && iron.details().get(1).name().component().getString().equals("First Stockroom"),
                "Each half should count once and preserve its own name and count");
        helper.assertTrue(entry(ownStock, Items.DIAMOND.getDescriptionId()).count() == 7
                        && entry(guestStock, Items.DIAMOND.getDescriptionId()).count() == 2 && guestStock.unavailable() == 1,
                "An authorized player should see the framed contents and another player should see only public storage");
        var hit = new BlockHitResult(Vec3.atCenterOf(left.getBlockPos()), Direction.UP, left.getBlockPos(), false);
        item.onItemUseFirst(catalog, new UseOnContext(owner, InteractionHand.OFF_HAND, hit));
        helper.assertTrue(entry(CatalogContents.scan(helper.getLevel(), owner, catalog), Items.IRON_INGOT.getDescriptionId())
                        .count() == 32,
                "Crouch-using the same half should remove its record");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catalogPreservesNamesAndSkipsUnloadedAndUnopenedStorage(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(LEFT, Blocks.CHEST);
        var chest = (ChestBlockEntity) helper.getBlockEntity(LEFT);
        chest.setLootTable(ResourceLocation.withDefaultNamespace("chests/simple_dungeon"), 1);
        var player = helper.makeMockSurvivalPlayer();
        var catalog = new ItemStack(ModItems.STOCKROOM_CATALOG.get()).setHoverName(Component.literal("Catalog"));
        player.setItemInHand(InteractionHand.MAIN_HAND, catalog);
        var menu = new StockroomCatalogMenu(1, player, InteractionHand.MAIN_HAND);
        BlockPos remote = new BlockPos(3_000_000, 64, 3_000_000);
        CatalogContents.toggle(catalog, GlobalPos.of(level.dimension(), chest.getBlockPos()));
        CatalogContents.toggle(catalog, GlobalPos.of(level.dimension(), remote));
        var result = CatalogContents.scan(level, player, catalog);
        helper.assertTrue(result.entries().isEmpty() && result.unavailable() == 2
                        && level.getChunkSource().getChunkNow(remote.getX() >> 4, remote.getZ() >> 4) == null
                        && chest.saveWithoutMetadata().contains("LootTable"),
                "A catalog scan should leave unloaded chunks and unopened structure loot alone");
        helper.assertTrue(menu.rename(player, "East Wing") && catalog.getHoverName().getString().equals("East Wing"),
                "The held catalog's title should persist on its item");
        player.setItemInHand(InteractionHand.MAIN_HAND, catalog.copy());
        helper.assertTrue(!menu.rename(player, "Wrong Book") && !menu.stillValid(player),
                "Replacing the held catalog should invalidate the open editor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catalogStreamsLargeSourcesAndKeepsLongCounts(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var menu = new StockroomCatalogMenu(1, player, InteractionHand.MAIN_HAND);
        var pos = GlobalPos.of(Level.NETHER, helper.absolutePos(LEFT));
        var counts = new LinkedHashMap<String, Long>();
        for (int i = 0; i < 70; i++) {
            counts.put("item.bibliocraft.test." + i, 2_147_483_640L + i);
        }
        var source = new CatalogContents.Source(pos, new CatalogContents.Name("Deep Storage", false), counts);
        menu.accept(new ModNetwork.StockLogPacket(1, 1, 2, false, null));
        for (var batch : source.batches()) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                batch.write(buffer);
                menu.accept(new ModNetwork.StockLogPacket(1, 0, 0, false, CatalogContents.Source.read(buffer)));
                helper.assertTrue(buffer.readableBytes() == 0, "A source should exactly consume its wire data");
            } finally {
                buffer.release();
            }
        }
        menu.accept(new ModNetwork.StockLogPacket(1, 0, 0, true, null));
        var largest = menu.entries().stream().filter(entry -> entry.item().endsWith(".69")).findFirst().orElseThrow();
        helper.assertTrue(menu.entries().size() == 70 && menu.sourceCount() == 1 && menu.unavailable() == 2
                        && largest.count() == counts.get("item.bibliocraft.test.69")
                        && largest.details().get(0).location().equals(pos),
                "A large inventory should keep every row, count, and dimension across its batches");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void compassKeepsTargetsAndSelectsAuthoredCardinalNeedles(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var compass = new ItemStack(ModItems.WAYPOINT_COMPASS.get());
        helper.assertTrue(WaypointCompassItem.getTarget(compass).equals(WaypointCompassItem.ORIGIN),
                "A placeholder compass should default to the world origin");
        player.setItemInHand(InteractionHand.OFF_HAND, compass);
        var menu = new WaypointCompassMenu(1, player, InteractionHand.OFF_HAND, WaypointCompassItem.ORIGIN);
        helper.assertTrue(!menu.save(player, Integer.MAX_VALUE, -2, "Invalid", false), "Out-of-range targets should be rejected");
        helper.assertTrue(menu.save(player, -25, 64, "North Store", false), "The held compass should accept a valid waypoint");
        var loaded = ItemStack.of(compass.save(new net.minecraft.nbt.CompoundTag()));
        var target = WaypointCompassItem.getTarget(loaded);
        helper.assertTrue(target.name().equals("North Store") && target.x() == -25 && target.z() == 64
                        && target.dimension().equals(helper.getLevel().dimension().location()),
                "Waypoint data should survive an item save and reload");
        var south = new WaypointCompassItem.Target(0, 10, "South", null);
        Map<Float, Integer> expectedParts = Map.of(0F, 5, 90F, 32, 180F, 23, 270F, 14);
        expectedParts.forEach((yaw, part) -> helper.assertTrue(
                WaypointCompassItem.needlePart(0.5, 0.5, yaw, south) == part,
                "The red tip should face the target at yaw " + yaw));
        player.setItemInHand(InteractionHand.OFF_HAND, compass.copy());
        helper.assertTrue(!menu.save(player, 2, 3, "Different Compass", false),
                "A stale editor should not change a replacement compass");
        helper.succeed();
    }

    private static CatalogContents.Entry entry(CatalogContents.Snapshot snapshot, String key) {
        return snapshot.entries().stream().filter(entry -> entry.item().equals(key)).findFirst().orElseThrow();
    }
}
