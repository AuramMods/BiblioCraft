package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.ClockBlock;
import art.arcane.bibliocraft.block.ClockPart;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.blockentity.ClockBlockEntity;
import art.arcane.bibliocraft.blockentity.ClockSettings;
import art.arcane.bibliocraft.menu.ClockMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModSounds;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class ClockGameTests {
    private static final BlockPos BASE = new BlockPos(2, 1, 2);
    private static final BlockPos TOP = BASE.above();
    private static final FurnitureMaterial FRAME =
            new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("bricks"));

    private ClockGameTests() {}

    @GameTest(template = "empty")
    public static void drillsJoinShiftSeparateAndKeepTheClocksIndependent(GameTestHelper helper) {
        var level = helper.getLevel();
        var lower = placeClock(helper, BASE);
        var upper = placeClock(helper, TOP);
        var player = helper.makeMockSurvivalPlayer();
        ItemStack hand = new ItemStack(ModItems.HAND_DRILL.get());
        ItemStack powered = new ItemStack(ModItems.BIBLIO_DRILL.get());
        var lowerSettings = new ClockSettings(0b1, 0, false, true, false, true);
        var upperSettings = new ClockSettings(0b10, 0b100, true, true, true, true);
        lower.setSettings(lowerSettings);
        upper.setSettings(upperSettings);

        player.setShiftKeyDown(true);
        drill(helper, player, hand, BASE);
        helper.assertBlockProperty(BASE, ClockBlock.PART, ClockPart.WALL);
        drill(helper, player, hand, TOP);
        helper.assertBlockProperty(BASE, ClockBlock.PART, ClockPart.LOWER);
        helper.assertBlockProperty(TOP, ClockBlock.PART, ClockPart.UPPER);
        helper.assertTrue(ClockBlock.mainClock(level, lower.getBlockPos()) == upper,
                "A joined base should resolve settings and power to its upper clock");
        helper.assertTrue(upper.getRenderBoundingBox().minY <= lower.getBlockPos().getY(),
                "The grandfather clock's visible case must cover both blocks");
        player.setShiftKeyDown(false);
        for (int depth : new int[]{1, 2, 0}) {
            drill(helper, player, powered, TOP);
            helper.assertBlockProperty(BASE, ClockBlock.SHIFT, depth);
            helper.assertBlockProperty(TOP, ClockBlock.SHIFT, depth);
        }
        player.setShiftKeyDown(true);
        drill(helper, player, powered, TOP);
        drill(helper, player, powered, BASE);
        helper.assertBlockProperty(BASE, ClockBlock.PART, ClockPart.WALL);
        helper.assertBlockProperty(TOP, ClockBlock.PART, ClockPart.WALL);
        helper.assertTrue(lowerSettings.equals(lower.getSettings()) && upperSettings.equals(upper.getSettings()),
                "Joining and separating must keep the two original clocks' own schedules");

        drill(helper, player, hand, BASE);
        drill(helper, player, hand, TOP);
        level.destroyBlock(lower.getBlockPos(), true);
        helper.assertBlockProperty(TOP, ClockBlock.PART, ClockPart.WALL);
        helper.assertTrue(upperSettings.equals(upper.getSettings()), "Breaking the base should restore the surviving wall clock");
        helper.succeedWhen(() -> helper.assertItemEntityCountIs(ModBlocks.CLOCK.get().asItem(), BASE, 2, 1));
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void bothGrandfatherHalvesEmitOnePulseThroughStoneThenHoldSolidOutput(GameTestHelper helper) {
        var level = helper.getLevel();
        var lower = placeClock(helper, BASE);
        var upper = placeClock(helper, TOP);
        var player = helper.makeMockSurvivalPlayer();
        player.setShiftKeyDown(true);
        ItemStack drill = new ItemStack(ModItems.HAND_DRILL.get());
        drill(helper, player, drill, BASE);
        drill(helper, player, drill, TOP);
        helper.setBlock(BASE.east(), Blocks.STONE);
        helper.setBlock(BASE.east().east(), Blocks.REDSTONE_LAMP);
        helper.setBlock(TOP.east(), Blocks.REDSTONE_LAMP);
        long original = level.getDayTime();
        long start = original - Math.floorMod(original, ClockSettings.SLOT_TICKS);
        level.setDayTime(start);
        long schedule = 1L << ClockSettings.slot(start);
        upper.setSettings(new ClockSettings(0, schedule, false, false, true, true));
        upper.tick();
        helper.assertBlockProperty(BASE.east().east(), RedstoneLampBlock.LIT, true);
        helper.assertBlockProperty(TOP.east(), RedstoneLampBlock.LIT, true);
        var tag = upper.saveWithFullMetadata();
        var reload = (ClockBlockEntity) BlockEntity.loadStatic(upper.getBlockPos(), upper.getBlockState(), tag);
        reload.setLevel(level);
        reload.tick();
        helper.assertTrue(reload.isPowered() && reload.saveWithoutMetadata().getLong("PulseUntil") == tag.getLong("PulseUntil"),
                "A reload during a pulse must resume the same deadline");

        helper.startSequence().thenIdle(25).thenExecute(() -> {
            helper.assertTrue(upper.isPowered(), "Output must stay active beyond one second");
        }).thenIdle(22).thenExecute(() -> {
            helper.assertBlockProperty(BASE.east().east(), RedstoneLampBlock.LIT, false);
            helper.assertBlockProperty(TOP.east(), RedstoneLampBlock.LIT, false);
            helper.assertTrue(!upper.isPowered(), "The selected half-hour must emit one two-second pulse");
            upper.setSettings(new ClockSettings(0, schedule, false, false, true, false));
            upper.tick();
            helper.assertBlockProperty(BASE.east().east(), RedstoneLampBlock.LIT, true);
            helper.assertBlockProperty(TOP.east(), RedstoneLampBlock.LIT, true);
            level.setDayTime(start + ClockSettings.SLOT_TICKS);
            upper.tick();
        }).thenIdle(5).thenExecute(() -> {
            helper.assertBlockProperty(BASE.east().east(), RedstoneLampBlock.LIT, false);
            helper.assertBlockProperty(TOP.east(), RedstoneLampBlock.LIT, false);
            level.setDayTime(original + helper.getTick());
        }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void clockSoundsFollowWorldTimeAndReloadDoesNotRepeatTheChime(GameTestHelper helper) {
        var level = helper.getLevel();
        var clock = placeClock(helper, BASE);
        long previous = level.getDayTime();
        List<String> sounds = new ArrayList<>();
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == level && event.getSound() != null && event.getSource() == SoundSource.BLOCKS
                    && BlockPos.containing(event.getPosition()).equals(clock.getBlockPos())) {
                sounds.add(event.getSound().value().getLocation().getPath());
            }
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            helper.assertTrue(ClockSettings.slot(0) == 12 && ClockSettings.slot(6000) == 24
                            && ClockSettings.slot(12000) == 36 && ClockSettings.slot(18000) == 0,
                    "The 24-hour dial must align sunrise, noon, dusk, and midnight with the schedule");
            clock.setSettings(new ClockSettings((1L << 12) | (1L << 13), 0, true, true, false, true));
            level.setDayTime(17);
            clock.tick();
            clock.tick();
            level.setDayTime(37);
            clock.tick();
            helper.assertTrue(sounds.equals(List.of("tick", "woundchime", "tock")),
                    "Clock should tick, chime once, and tock at the recorded phases");
            var reload = (ClockBlockEntity) BlockEntity.loadStatic(clock.getBlockPos(), clock.getBlockState(), clock.saveWithFullMetadata());
            reload.setLevel(level);
            level.setDayTime(40);
            reload.tick();
            helper.assertTrue(sounds.size() == 3, "Reopening a clock in the same half-hour should keep an already played chime quiet");
            level.setDayTime(500);
            reload.tick();
            helper.assertTrue(sounds.size() == 4 && sounds.get(3).equals(ModSounds.WOUNDCHIME.get().getLocation().getPath()),
                    "The next selected half-hour should start another chime");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
            level.setDayTime(previous);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clockMenuPreservesConcurrentEditsAndChecksRangeAndLock(GameTestHelper helper) {
        var clock = placeClock(helper, BASE);
        var owner = helper.makeMockSurvivalPlayer();
        var visitor = helper.makeMockSurvivalPlayer();
        var first = openClock(owner, clock);
        var second = openClock(visitor, clock);
        var edit1 = new ClockSettings(1L << 47, 0, true, true, false, true);
        var edit2 = new ClockSettings(0, 1L << 12, false, false, true, false);
        helper.assertTrue(!first.save(visitor, edit1) && first.save(owner, edit1) && second.save(visitor, edit2),
                "Clock updates should belong to the currently open menu");
        var expected = new ClockSettings(edit1.chimes(), edit2.redstone(), false, true, true, false);
        helper.assertTrue(expected.equals(clock.getSettings()), "Independent schedule and switch changes should merge");
        var client = new ClockBlockEntity(clock.getBlockPos(), clock.getBlockState());
        client.handleUpdateTag(clock.getUpdateTag());
        var wire = new FriendlyByteBuf(Unpooled.buffer());
        try {
            expected.write(wire);
            helper.assertTrue(expected.equals(ClockSettings.read(wire)) && expected.equals(client.getSettings()),
                    "The full 48-slot schedules and switches should survive synchronization");
        } finally {
            wire.release();
        }
        var lockedSession = openClock(visitor, clock);
        clock.toggleLock(owner);
        helper.assertTrue(!lockedSession.save(visitor, edit2), "A lock must revoke permission to edit an open menu");
        var distantSession = openClock(owner, clock);
        owner.setPos(Vec3.atCenterOf(clock.getBlockPos()).add(12, 0, 0));
        helper.assertTrue(!distantSession.save(owner, edit2), "Leaving the clock's range must reject a delayed update");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clockRecipesAndRelocationKeepWoodPanelAndSchedule(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            var slab = wood == FurnitureMaterial.Wood.FRAMED ? ModItems.FRAMING_BOARD.get()
                    : ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
            for (int i : new int[]{0, 2, 3, 5, 6, 8}) {
                grid.setItem(i, new ItemStack(slab));
            }
            grid.setItem(1, new ItemStack(Items.CLOCK));
            grid.setItem(4, new ItemStack(Items.STICK));
            grid.setItem(7, new ItemStack(Items.GOLD_INGOT));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack result = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(result.is(ModBlocks.CLOCK.get().asItem()) && FurnitureMaterial.fromItem(result).wood() == wood,
                    "The original recipe should craft a clock in each material");
        }
        helper.setBlock(BASE.below(), Blocks.STONE);
        BlockPos absolute = helper.absolutePos(BASE);
        var frame = FRAME.applyTo(new ItemStack(ModBlocks.CLOCK.get()));
        placeItem(player, frame, absolute.below());
        var clock = (ClockBlockEntity) helper.getBlockEntity(BASE);
        var settings = new ClockSettings(0x22, 0x55, false, true, true, false);
        clock.setSettings(settings);
        var picked = clock.getBlockState().getBlock().getCloneItemStack(level, absolute, clock.getBlockState());
        helper.assertTrue(FRAME.equals(FurnitureMaterial.fromItem(picked))
                        && settings.equals(ClockSettings.load(picked.getTagElement("BlockEntityTag").getCompound(ClockSettings.TAG))),
                "Pick-block should copy a configured clock's material and schedule");
        level.destroyBlock(absolute, true);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(1.5),
                entity -> entity.getItem().is(ModBlocks.CLOCK.get().asItem()));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().getCount() == 1,
                "Breaking one clock must drop exactly one clock");
        var drop = drops.get(0).getItem().copy();
        drops.get(0).discard();
        placeItem(player, drop, absolute.below());
        var replaced = (ClockBlockEntity) helper.getBlockEntity(BASE);
        helper.assertTrue(FRAME.equals(replaced.getMaterial()) && settings.equals(replaced.getSettings()),
                "Replacing a harvested clock should restore both its schedule and custom frame");
        helper.succeed();
    }

    private static ClockBlockEntity placeClock(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.CLOCK.get());
        return (ClockBlockEntity) helper.getBlockEntity(pos);
    }

    private static ClockMenu openClock(Player player, ClockBlockEntity clock) {
        player.setPos(Vec3.atCenterOf(clock.getBlockPos()));
        var menu = new ClockMenu(1, clock, clock);
        player.containerMenu = menu;
        return menu;
    }

    private static void drill(GameTestHelper helper, Player player, ItemStack drill, BlockPos relative) {
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        drill.getItem().onItemUseFirst(drill, context(player, helper.absolutePos(relative), Direction.EAST));
    }

    private static void placeItem(Player player, ItemStack stack, BlockPos support) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(context(player, support, Direction.UP));
    }

    private static UseOnContext context(Player player, BlockPos pos, Direction side) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).relative(side, 0.5), side, pos, false));
    }
}
