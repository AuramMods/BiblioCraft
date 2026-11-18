package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.DeskBellBlockEntity;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class DeskBellGameTests {
    private static final BlockPos BELL = new BlockPos(2, 1, 2);

    private DeskBellGameTests() {}

    @GameTest(template = "empty")
    public static void deskBellRingsOnClickAndHonorsLocks(GameTestHelper helper) {
        helper.setBlock(BELL, ModBlocks.BELL.get());
        var bell = (DeskBellBlockEntity) helper.getBlockEntity(BELL);
        var owner = helper.makeMockSurvivalPlayer();
        var visitor = helper.makeMockSurvivalPlayer();
        BlockPos pos = bell.getBlockPos();
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0, 0.18, 0), Direction.UP, pos, false);

        withDings(helper, count -> {
            helper.useBlock(BELL, visitor, hit);
            helper.assertTrue(count.get() == 1, "A regular right-click should play one bell sound");
            bell.toggleLock(owner);
            helper.useBlock(BELL, visitor, hit);
            helper.assertTrue(count.get() == 1, "A visitor should be unable to ring a locked bell by hand");
            helper.useBlock(BELL, owner, hit);
            helper.assertTrue(count.get() == 2, "The owner should still be able to ring it");
        });

        bell.toggleLock(owner);
        helper.getLevel().destroyBlock(pos, true);
        helper.succeedWhen(() -> helper.assertItemEntityCountIs(ModBlocks.BELL.get().asItem(), BELL, 2, 1));
    }

    @GameTest(template = "empty")
    public static void deskBellRingsOnAnalogIncreasesAndSavesItsLastSignal(GameTestHelper helper) {
        helper.setBlock(BELL, ModBlocks.BELL.get());
        helper.setBlock(BELL.west(), Blocks.STONE);
        var bell = (DeskBellBlockEntity) helper.getBlockEntity(BELL);
        BlockPos pos = bell.getBlockPos();
        var level = helper.getLevel();
        BlockPos source = BELL.west().above();
        var plate = Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState();
        var ticker = bell.getBlockState().getTicker(level, ModBlockEntities.BELL.get());

        withDings(helper, count -> {
            helper.setBlock(source, plate.setValue(WeightedPressurePlateBlock.POWER, 3));
            ticker.tick(level, pos, bell.getBlockState(), bell);
            ticker.tick(level, pos, bell.getBlockState(), bell);
            helper.assertTrue(count.get() == 0, "The first two ticks should not sample power");
            ticker.tick(level, pos, bell.getBlockState(), bell);
            helper.assertTrue(count.get() == 1, "The bell should hear a rise through the adjacent stone block");
            sample(helper, bell);
            helper.setBlock(source, plate.setValue(WeightedPressurePlateBlock.POWER, 2));
            sample(helper, bell);
            helper.assertTrue(count.get() == 1, "Held or falling power should stay quiet");
            helper.setBlock(source, plate.setValue(WeightedPressurePlateBlock.POWER, 7));
            sample(helper, bell);
            helper.assertTrue(count.get() == 2, "Increasing an already positive signal should ring again");

            var restored = (DeskBellBlockEntity) BlockEntity.loadStatic(pos, bell.getBlockState(), bell.saveWithFullMetadata());
            sample(helper, restored);
            helper.assertTrue(count.get() == 2, "Reloading under steady power should stay quiet");
            helper.setBlock(source, Blocks.AIR);
            sample(helper, restored);
            helper.setBlock(source, plate.setValue(WeightedPressurePlateBlock.POWER, 7));
            sample(helper, restored);
            helper.assertTrue(count.get() == 3, "Removing power should rearm the bell");
        });
        helper.succeed();
    }

    private static void sample(GameTestHelper helper, DeskBellBlockEntity bell) {
        for (int tick = 0; tick < 3; tick++) {
            bell.tick(helper.getLevel(), bell.getBlockPos());
        }
    }

    private static void withDings(GameTestHelper helper, Consumer<AtomicInteger> test) {
        AtomicInteger count = new AtomicInteger();
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == helper.getLevel() && event.getSound() != null
                    && event.getSound().value() == ModSounds.DING.get()
                    && event.getSource() == SoundSource.BLOCKS
                    && BlockPos.containing(event.getPosition()).equals(helper.absolutePos(BELL))) {
                count.incrementAndGet();
            }
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            test.accept(count);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
    }
}
