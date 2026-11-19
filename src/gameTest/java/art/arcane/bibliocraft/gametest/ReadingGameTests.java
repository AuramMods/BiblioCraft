package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FurnitureReading;
import art.arcane.bibliocraft.block.LabelBlock;
import art.arcane.bibliocraft.block.ShelfBlock;
import art.arcane.bibliocraft.blockentity.FurnitureInventoryBlockEntity;
import art.arcane.bibliocraft.blockentity.TypesettingTableBlockEntity;
import art.arcane.bibliocraft.book.PrintingLibrary;
import art.arcane.bibliocraft.enchantment.ReadingEnchantment;
import art.arcane.bibliocraft.item.ReadingGlassesItem;
import art.arcane.bibliocraft.item.ReadingGlassesItem.Variant;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModEnchantments;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class ReadingGameTests {
    private static final BlockPos FURNITURE = new BlockPos(2, 1, 2);

    private ReadingGameTests() {}

    @GameTest(template = "empty")
    public static void glassesRecipesPreserveTheThreeWearableVariants(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        grid.setItem(0, new ItemStack(Items.GLASS_PANE));
        grid.setItem(1, new ItemStack(Items.IRON_INGOT));
        grid.setItem(2, new ItemStack(Items.GLASS_PANE));
        grid.setItem(3, new ItemStack(Items.BLACK_DYE));
        grid.setItem(5, new ItemStack(Items.BLACK_DYE));
        ItemStack glasses = craft(helper, grid);
        assertGlasses(helper, glasses, Variant.READING, "glasses.png");
        glasses.setHoverName(Component.literal("Library glasses"));
        glasses.enchant(ModEnchantments.READING.get(), 1);

        var handGrid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 2, 2);
        handGrid.setItem(0, glasses);
        handGrid.setItem(3, new ItemStack(Items.GRAY_DYE));
        ItemStack tinted = craft(helper, handGrid);
        assertGlasses(helper, tinted, Variant.TINTED, "glasses_darktint.png");
        helper.assertTrue(tinted.getHoverName().equals(glasses.getHoverName())
                        && tinted.getEnchantmentLevel(ModEnchantments.READING.get()) == 1,
                "Tinting keeps a custom name and enchantments");
        handGrid.setItem(0, tinted);
        helper.assertTrue(craft(helper, handGrid).isEmpty(), "Tinted glasses cannot be tinted again");

        grid.clearContent();
        for (int slot : new int[] {1, 3, 5, 7}) {
            grid.setItem(slot, new ItemStack(Items.GOLD_NUGGET));
        }
        grid.setItem(4, new ItemStack(Items.GLASS_PANE));
        ItemStack monocle = craft(helper, grid);
        assertGlasses(helper, monocle, Variant.MONOCLE, "glasses_mono.png");
        handGrid.setItem(0, monocle);
        helper.assertTrue(craft(helper, handGrid).isEmpty(), "The monocle cannot be tinted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void readingAppearsOnBooksAndAppliesToHelmetsAtAnAnvil(GameTestHelper helper) {
        var reading = ModEnchantments.READING.get();
        helper.assertTrue(reading.getMinCost(1) == 10 && reading.getMaxCost(1) == 61,
                "Reading should keep the legacy enchanting power range");
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helper.assertTrue(EnchantmentHelper.getAvailableEnchantmentResults(30, new ItemStack(Items.BOOK), false)
                        .stream().anyMatch(option -> option.enchantment == reading)
                        && EnchantmentHelper.getAvailableEnchantmentResults(30, helmet, false)
                        .stream().noneMatch(option -> option.enchantment == reading),
                "The enchanting table should offer Reading on books only");

        var player = helper.makeMockSurvivalPlayer();
        helper.setBlock(FURNITURE, Blocks.ANVIL);
        var access = ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(FURNITURE));
        var anvil = new AnvilMenu(1, player.getInventory(), access);
        ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(reading, 1));
        helper.assertTrue(reading.canEnchant(helmet) && EnchantmentHelper.getEnchantments(book).containsKey(reading),
                "The anvil inputs must contain a compatible helmet and a stored Reading enchantment");
        anvil.getSlot(0).set(new ItemStack(Items.IRON_CHESTPLATE));
        anvil.getSlot(1).set(book.copy());
        helper.assertTrue(anvil.getSlot(2).getItem().isEmpty(), "Reading cannot be applied to a chestplate");
        anvil = new AnvilMenu(2, player.getInventory(), access);
        anvil.getSlot(0).set(helmet);
        anvil.getSlot(1).set(book.copy());
        anvil.createResult();
        ItemStack result = anvil.getSlot(2).getItem();
        helper.assertTrue(ReadingEnchantment.canRead(result),
                "The anvil result should have Reading, found " + result.save(new CompoundTag()) + " at cost " + anvil.getCost());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void readingTargetsVisibleShelfAndLabelSlots(GameTestHelper helper) {
        var owner = helper.makeMockSurvivalPlayer();
        var guest = helper.makeMockSurvivalPlayer();
        ItemStack glasses = new ItemStack(ModItems.BIBLIO_GLASSES.get());
        owner.setItemSlot(EquipmentSlot.HEAD, glasses.copy());
        BlockPos pos = helper.absolutePos(FURNITURE);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var opening = facing.getClockWise();
            var right = opening.getCounterClockWise();
            helper.setBlock(FURNITURE, ModBlocks.SHELF.get().defaultBlockState().setValue(ShelfBlock.FACING, facing));
            var shelf = (FurnitureInventoryBlockEntity) helper.getBlockEntity(FURNITURE);
            for (int slot = 0; slot < 4; slot++) {
                var point = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(opening.getNormal()).scale(0.45))
                        .add(Vec3.atLowerCornerOf(right.getNormal()).scale(slot % 2 == 0 ? -0.25 : 0.25))
                        .add(0, slot < 2 ? 0.25 : -0.25, 0);
                var target = FurnitureReading.target(helper.getLevel(), new BlockHitResult(point, opening, pos, false));
                helper.assertTrue(target != null && target.slot() == slot, "Reading should target the visible shelf slot");
                shelf.setItem(slot, new ItemStack(Items.PAPER, 7).setHoverName(Component.literal("Shelf " + slot)));
                helper.assertTrue(hasText(FurnitureReading.read(owner, target), "7", "Shelf " + slot),
                        "The readout should include the live name and count");
                guest.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                guest.setItemSlot(EquipmentSlot.MAINHAND, glasses.copy());
                helper.assertTrue(FurnitureReading.read(guest, target).isEmpty(), "Glasses must be worn to read");
                shelf.toggleLock(owner);
                guest.setItemSlot(EquipmentSlot.HEAD, glasses.copy());
                helper.assertTrue(FurnitureReading.read(guest, target).isEmpty()
                                && !FurnitureReading.read(owner, target).isEmpty(), "Reading should honor furniture ownership");
                shelf.toggleLock(owner);
            }

            helper.setBlock(FURNITURE, ModBlocks.LABEL.get().defaultBlockState().setValue(LabelBlock.FACING, facing));
            var label = (FurnitureInventoryBlockEntity) helper.getBlockEntity(FURNITURE);
            label.setItem(0, new ItemStack(Items.APPLE).setHoverName(Component.literal("Middle")));
            var labelHit = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(opening.getNormal()).scale(-0.45))
                    .add(Vec3.atLowerCornerOf(right.getNormal()).scale(-0.14));
            var target = FurnitureReading.target(helper.getLevel(), new BlockHitResult(labelHit, opening, pos, false));
            helper.assertTrue(hasText(FurnitureReading.read(owner, target), "Middle"), "One icon fills the label");
            label.setItem(1, new ItemStack(Items.GOLD_INGOT).setHoverName(Component.literal("Left")));
            label.setItem(2, new ItemStack(Items.IRON_INGOT).setHoverName(Component.literal("Right")));
            helper.assertTrue(hasText(FurnitureReading.read(owner, target), "Left"),
                    "Reading should follow the left icon on a rotated label");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void typesettingReadoutReportsCostsAndHonorsLibraryPrivacy(GameTestHelper helper) {
        helper.setBlock(FURNITURE, ModBlocks.TYPESETTING_TABLE.get());
        var table = (TypesettingTableBlockEntity) helper.getBlockEntity(FURNITURE);
        var owner = helper.makeMockSurvivalPlayer();
        var guest = helper.makeMockSurvivalPlayer();
        owner.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.BIBLIO_GLASSES.get()));
        guest.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.BIBLIO_GLASSES.get()));
        BlockPos pos = helper.absolutePos(FURNITURE);
        var sourceHit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.8, 0.95, 0.7), Direction.UP, pos, false);
        var source = FurnitureReading.target(helper.getLevel(), sourceHit);
        helper.assertTrue(source != null && source.slot() == TypesettingTableBlockEntity.SOURCE,
                "The readout should target the typesetting source slot");
        table.setItem(TypesettingTableBlockEntity.SOURCE, EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(Enchantments.MENDING, 1)));
        var cost = FurnitureReading.read(owner, source);
        helper.assertTrue(hasText(cost, Integer.toString(table.sourceCost())) && hasText(cost, "Mending"),
                "The readout should show the experience cost and stored enchantment");

        var signed = new ItemStack(Items.WRITTEN_BOOK);
        signed.getOrCreateTag().putString("title", "Private readout");
        signed.getOrCreateTag().putString("author", owner.getGameProfile().getName());
        var pages = new ListTag();
        pages.add(StringTag.valueOf("\"Text\""));
        signed.getOrCreateTag().put("pages", pages);
        var library = PrintingLibrary.get(helper.getLevel());
        var entry = library.publish(signed, owner);
        helper.assertTrue(entry != null, "The signed book should publish");
        if (entry.shared()) {
            library.toggleVisibility(entry.id(), owner);
        }
        table.select(entry.id());
        table.setItem(TypesettingTableBlockEntity.SOURCE, ItemStack.EMPTY);
        helper.assertTrue(hasText(FurnitureReading.read(owner, source), "Private readout")
                        && FurnitureReading.read(guest, source).isEmpty(), "Only the owner can read a private selection");
        library.toggleVisibility(entry.id(), owner);
        helper.assertTrue(hasText(FurnitureReading.read(guest, source), "Private readout"),
                "Sharing makes the selection visible to the guest");
        library.delete(entry.id(), owner);
        helper.succeed();
    }

    private static boolean hasText(List<Component> lines, String... words) {
        return lines.stream().map(ReadingGameTests::textWithArguments)
                .anyMatch(line -> Arrays.stream(words).allMatch(line::contains));
    }

    private static String textWithArguments(Component line) {
        return line.getContents() instanceof TranslatableContents translation
                ? line.getString() + Arrays.toString(translation.getArgs()) : line.getString();
    }

    private static ItemStack craft(GameTestHelper helper, TransientCraftingContainer grid) {
        var level = helper.getLevel();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
                .map(recipe -> recipe.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static void assertGlasses(GameTestHelper helper, ItemStack stack, Variant variant, String texture) {
        var loaded = ItemStack.of(stack.save(new CompoundTag()));
        helper.assertTrue(loaded.getItem() instanceof ReadingGlassesItem
                        && ReadingGlassesItem.variant(loaded) == variant
                        && LivingEntity.getEquipmentSlotForItem(loaded) == EquipmentSlot.HEAD
                        && !loaded.isDamageableItem() && loaded.getMaxStackSize() == 1
                        && ((ReadingGlassesItem) loaded.getItem()).getArmorTexture(loaded, null, EquipmentSlot.HEAD, null)
                        .equals("bibliocraft:textures/armor/" + texture),
                "The recipe should produce wearable " + variant + " with its original texture after saving");
    }
}
