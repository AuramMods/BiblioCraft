package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.CookieJarBlock;
import art.arcane.bibliocraft.blockentity.CookieJarBlockEntity;
import art.arcane.bibliocraft.menu.CookieJarMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class CookieJarGameTests {
    private static final BlockPos JAR = new BlockPos(2, 1, 2);

    private CookieJarGameTests() {}

    @GameTest(template = "empty")
    public static void cookieJarClickPulsesRedstoneWhileItsMenuStaysAvailable(GameTestHelper helper) {
        helper.setBlock(JAR, ModBlocks.COOKIE_JAR.get());
        helper.setBlock(JAR.west(), Blocks.STONE);
        helper.setBlock(JAR.west(2), Blocks.REDSTONE_LAMP);
        BlockPos pos = helper.absolutePos(JAR);
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5);
        var jar = (CookieJarBlockEntity) helper.getBlockEntity(JAR);
        var menu = new CookieJarMenu(1, player.getInventory(), jar);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        helper.useBlock(JAR, player, hit);
        helper.assertBlockProperty(JAR, CookieJarBlock.OPEN, true);
        helper.assertBlockProperty(JAR.west(2), RedstoneLampBlock.LIT, true);
        var reloaded = (CookieJarBlockEntity) BlockEntity.loadStatic(pos, jar.getBlockState(), jar.saveWithFullMetadata());
        helper.assertTrue(reloaded != null && reloaded.getPulseEnd() == helper.getLevel().getGameTime() + 20,
                "The jar should persist the end of a pulse in progress");
        helper.runAfterDelay(10, () -> {
            helper.assertBlockProperty(JAR, CookieJarBlock.OPEN, true);
            helper.useBlock(JAR, player, hit);
        });
        helper.runAfterDelay(26, () -> {
            helper.assertBlockProperty(JAR, CookieJarBlock.OPEN, false);
            helper.assertBlockProperty(JAR.west(2), RedstoneLampBlock.LIT, false);
            helper.assertTrue(menu.stillValid(player), "The pulse ending should leave a nearby player's menu valid");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void aReplacedCookieJarGetsItsOwnFullPulse(GameTestHelper helper) {
        helper.setBlock(JAR, ModBlocks.COOKIE_JAR.get());
        BlockPos pos = helper.absolutePos(JAR);
        Player player = helper.makeMockSurvivalPlayer();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        helper.useBlock(JAR, player, hit);
        helper.runAfterDelay(10, () -> {
            helper.setBlock(JAR, Blocks.AIR);
            helper.setBlock(JAR, ModBlocks.COOKIE_JAR.get());
            helper.useBlock(JAR, player, hit);
        });
        helper.runAfterDelay(25, () -> helper.assertBlockProperty(JAR, CookieJarBlock.OPEN, true));
        helper.runAfterDelay(33, () -> {
            helper.assertBlockProperty(JAR, CookieJarBlock.OPEN, false);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void cookieJarKeepsItsContentsAndShowsOneMeshPerOccupiedSlot(GameTestHelper helper) {
        helper.setBlock(JAR, ModBlocks.COOKIE_JAR.get());
        CookieJarBlockEntity jar = (CookieJarBlockEntity) helper.getBlockEntity(JAR);
        Player player = helper.makeMockSurvivalPlayer();
        var menu = new CookieJarMenu(1, player.getInventory(), jar);
        player.getInventory().setItem(9, new ItemStack(Items.COOKIE, 5));
        menu.quickMoveStack(player, CookieJarBlockEntity.SLOT_COUNT);
        helper.assertTrue(jar.getItem(0).getCount() == 5 && player.getInventory().getItem(9).isEmpty(),
                "Shift-click should put the cookies into the jar");
        ItemStack keepsake = new ItemStack(Items.DIAMOND, 3).setHoverName(Component.literal("Keepsake"));
        jar.setItem(7, keepsake.copy());
        helper.assertTrue(jar.getModelData().get(CookieJarBlockEntity.VISIBLE_COOKIES) == 3,
                "Nonadjacent inventory slots should show the two lowest cookie meshes");

        var restored = (CookieJarBlockEntity) BlockEntity.loadStatic(jar.getBlockPos(), jar.getBlockState(),
                jar.saveWithFullMetadata());
        helper.assertTrue(restored != null && ItemStack.matches(keepsake, restored.getItem(7))
                        && restored.getItem(0).getCount() == 5,
                "Save and reload should preserve both slot contents and item data");
        var client = new CookieJarBlockEntity(jar.getBlockPos(), jar.getBlockState());
        client.handleUpdateTag(jar.getUpdateTag());
        helper.assertTrue(client.getModelData().get(CookieJarBlockEntity.VISIBLE_COOKIES) == 3,
                "The live update should include the two occupied slots");
        client.handleUpdateTag(new CompoundTag());
        helper.assertTrue(client.getModelData().get(CookieJarBlockEntity.VISIBLE_COOKIES) == 0,
                "An empty update should hide every cookie");

        helper.getLevel().destroyBlock(jar.getBlockPos(), true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.DIAMOND, JAR, 2, 3);
            helper.assertItemEntityCountIs(Items.COOKIE, JAR, 2, 5);
            helper.assertItemEntityCountIs(ModBlocks.COOKIE_JAR.get().asItem(), JAR, 2, 1);
        });
    }
}
