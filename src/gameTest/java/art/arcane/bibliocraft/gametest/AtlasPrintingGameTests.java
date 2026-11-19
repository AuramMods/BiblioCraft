package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.PrintingPressBlockEntity;
import art.arcane.bibliocraft.blockentity.TypesettingTableBlockEntity;
import art.arcane.bibliocraft.enchantment.DeathCompassEnchantment;
import art.arcane.bibliocraft.item.DeathCompassItem;
import art.arcane.bibliocraft.item.PrintingPlateItem;
import art.arcane.bibliocraft.map.AtlasContents;
import art.arcane.bibliocraft.map.MapPin;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModEnchantments;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class AtlasPrintingGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private AtlasPrintingGameTests() {}

    @GameTest(template = "empty")
    public static void typesettingChargesForMapsAndKeepsTheSourceAtlas(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.TYPESETTING_TABLE.get());
        var table = (TypesettingTableBlockEntity) helper.getBlockEntity(MACHINE);
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(MACHINE)).add(1, 0, 0));
        ItemStack source = atlas(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, source.copy());
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(table.getBlockPos()).add(.77, .92, .75),
                Direction.UP, table.getBlockPos(), false);
        helper.useBlock(MACHINE, player, hit);
        helper.assertTrue(player.getMainHandItem().isEmpty() && ItemStack.matches(source, table.getItem(TypesettingTableBlockEntity.SOURCE)),
                "The original source surface must accept one atlas");
        table.setItem(TypesettingTableBlockEntity.CHASES, new ItemStack(ModItems.BIBLIO_CHASE.get(), 2));
        player.experienceLevel = 4;
        player.setShiftKeyDown(true);
        helper.useBlock(MACHINE, player, hit);
        helper.assertTrue(table.getItem(TypesettingTableBlockEntity.RESULT).isEmpty() && table.getItem(TypesettingTableBlockEntity.CHASES).getCount() == 2,
                "An underfunded stamp must keep its chases and original");
        player.experienceLevel = 5;
        helper.useBlock(MACHINE, player, hit);
        ItemStack plate = table.getItem(TypesettingTableBlockEntity.RESULT);
        var copy = new AtlasContents(PrintingPlateItem.atlasCopy(plate));
        var original = new AtlasContents(source);
        helper.assertTrue(plate.is(ModItems.ATLAS_PLATE.get()) && plate.hasFoil() && player.experienceLevel == 0
                        && table.getItem(TypesettingTableBlockEntity.CHASES).getCount() == 1 && ItemStack.matches(table.getItem(TypesettingTableBlockEntity.SOURCE), source),
                "Stamping two maps costs five levels and one chase without removing the master atlas");
        helper.assertTrue(copy.filledMapCount() == 2 && copy.getItem(0).isEmpty() && copy.getItem(1).isEmpty()
                        && copy.getItem(8).isEmpty() && copy.selectedCompass() == -1 && copy.selected() == 6
                        && MapPin.read(copy.selectedMap()).equals(MapPin.read(original.selectedMap())),
                "The plate contains both maps and their pins while the original keeps loose supplies");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void anAtlasPlatePrintsOnceAndResumesItsSavedJob(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.PRINTING_PRESS.get());
        var press = (PrintingPressBlockEntity) helper.getBlockEntity(MACHINE);
        ItemStack source = atlas(helper);
        ItemStack plate = PrintingPlateItem.forAtlas(source);
        press.setItem(PrintingPressBlockEntity.PLATE, plate);
        press.setItem(PrintingPressBlockEntity.INK, new ItemStack(Items.BLACK_DYE, 2));
        press.setItem(PrintingPressBlockEntity.BLANK, new ItemStack(Items.BOOK));
        press.tick(helper.getLevel());
        helper.assertTrue(press.ticksLeft() == 0 && press.getItem(PrintingPressBlockEntity.INK).getCount() == 2
                        && !PrintingPressBlockEntity.accepts(PrintingPressBlockEntity.BLANK, source),
                "An atlas plate needs an empty atlas and must not consume ink for an ordinary book or erase a filled atlas");
        press.setItem(PrintingPressBlockEntity.BLANK, new ItemStack(ModItems.ATLAS_BOOK.get()));
        press.tick(helper.getLevel());
        press.setItem(PrintingPressBlockEntity.BLANK, new ItemStack(ModItems.ATLAS_BOOK.get()).setHoverName(Component.literal("Other atlas")));
        press.tick(helper.getLevel());
        helper.assertTrue(press.ticksLeft() == 0 && press.getItem(PrintingPressBlockEntity.PLATE) == plate,
                "Swapping the blank atlas must cancel without consuming the plate");
        press.tick(helper.getLevel());
        for (int i = 0; i < 7; i++) {
            press.tick(helper.getLevel());
        }
        int remaining = press.ticksLeft();
        press.load(press.saveWithFullMetadata());
        helper.assertTrue(remaining > 0 && press.ticksLeft() == remaining && press.saveWithFullMetadata().contains("JobAtlas"),
                "The atlas input and printing progress must survive reload");
        for (int i = 0; i < remaining; i++) {
            press.tick(helper.getLevel());
        }
        ItemStack printed = press.getItem(PrintingPressBlockEntity.RESULT);
        helper.assertTrue(ItemStack.matches(printed, PrintingPlateItem.atlasCopy(plate)) && press.getItem(PrintingPressBlockEntity.PLATE).isEmpty()
                        && press.getItem(PrintingPressBlockEntity.BLANK).isEmpty() && press.getItem(PrintingPressBlockEntity.INK).isEmpty(),
                "A resumed job must produce exactly one populated atlas and consume its plate and input");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCraftingManagerFindsBothAtlasEnchantingPatterns(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        for (int slot : new int[] {0, 2, 6, 8}) {
            grid.setItem(slot, new ItemStack(Items.ENDER_PEARL));
        }
        ItemStack source = atlas(helper);
        source.getOrCreateTag().putBoolean("bibliocraft:death_compass_enabled", true);
        grid.setItem(4, source);
        var level = helper.getLevel();
        var id = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "enchantedatlas");
        for (boolean vertical : List.of(true, false)) {
            for (int slot : new int[] {1, 3, 5, 7}) {
                boolean end = slot == 1 || slot == 7;
                grid.setItem(slot, vertical == end ? new ItemStack(ModItems.WAYPOINT_COMPASS.get())
                        : EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.MENDING, 1)));
            }
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElse(null);
            helper.assertTrue(recipe != null && recipe.getId().equals(id),
                    "The real crafting type must find both original layouts");
            var result = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.DEATH_COMPASS.get(), result) == 1
                            && !DeathCompassEnchantment.hasLegacyMarker(result) && DeathCompassEnchantment.hasLegacyMarker(source)
                            && new AtlasContents(result).filledMapCount() == 2,
                    "Upgrade should preserve the atlas and migrate its placeholder flag into the real enchantment");
        }
        helper.assertTrue(ModEnchantments.DEATH_COMPASS.get().canEnchant(source)
                        && !ModEnchantments.DEATH_COMPASS.get().canEnchant(new ItemStack(Items.DIAMOND_SWORD)),
                "Death Compass is an atlas enchantment");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyATrackedDeathGrantsOneLocatedCompassAfterRespawn(GameTestHelper helper) {
        Player original = helper.makeMockSurvivalPlayer();
        var inventory = original.getInventory();
        original.setPos(-25.3, 110, 456.8);
        inventory.setItem(9, atlas(helper));
        DeathCompassItem.recordDeath(original);
        DeathCompassItem.deliver(original);
        helper.assertTrue(inventory.countItem(ModItems.DEATH_COMPASS.get()) == 0, "A regular atlas must not grant a compass");
        inventory.setItem(9, DeathCompassEnchantment.apply(inventory.getItem(9)));
        DeathCompassItem.recordDeath(original);
        Player replacement = helper.makeMockSurvivalPlayer();
        replacement.getPersistentData().merge(original.getPersistentData().copy());
        DeathCompassItem.deliver(replacement);
        DeathCompassItem.deliver(replacement);
        var compass = replacement.getInventory().getItem(0);
        var target = DeathCompassItem.target(compass);
        helper.assertTrue(compass.is(ModItems.DEATH_COMPASS.get()) && replacement.getInventory().countItem(ModItems.DEATH_COMPASS.get()) == 1
                        && target != null && target.x() == -26 && target.z() == 456
                        && helper.getLevel().dimension().location().equals(target.dimension()) && compass.getTag().getInt("DeathY") == 110,
                "A respawn should receive one compass with the original coordinates and dimension after the saved-data transfer");
        helper.succeed();
    }

    private static ItemStack atlas(GameTestHelper helper) {
        var stack = new ItemStack(ModItems.ATLAS_BOOK.get()).setHoverName(Component.literal("Old Harbor"));
        var contents = new AtlasContents(stack);
        var map = MapItem.create(helper.getLevel(), 0, 0, (byte) 0, true, false);
        MapPin.write(map, List.of(new MapPin(UUID.randomUUID(), new MapPin.Point(.2, .72), "Old Harbor", DyeColor.CYAN)));
        contents.setItem(0, new ItemStack(Items.MAP, 4));
        contents.setItem(1, new ItemStack(ModItems.WAYPOINT_COMPASS.get()));
        contents.setItem(6, map);
        contents.setItem(7, MapItem.create(helper.getLevel(), 128, 0, (byte) 0, true, false));
        contents.setItem(8, new ItemStack(Items.MAP));
        contents.select(6);
        contents.selectCompass(1);
        contents.saveInventory();
        return stack;
    }
}
