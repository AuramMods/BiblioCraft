package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.HorizontalFacingEntityBlock;
import art.arcane.bibliocraft.block.PrintingPressBlock;
import art.arcane.bibliocraft.block.TypesettingTableBlock;
import art.arcane.bibliocraft.blockentity.PrintingPressBlockEntity;
import art.arcane.bibliocraft.blockentity.TypesettingTableBlockEntity;
import art.arcane.bibliocraft.book.PrintingLibrary;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.PrintingPlateItem;
import art.arcane.bibliocraft.menu.PrintingPressMenu;
import art.arcane.bibliocraft.menu.TypesettingTableMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class PrintingGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private PrintingGameTests() {}

    @GameTest(template = "empty")
    public static void typeTableUsesAuthoredSlotsAndPoweredPlateStamp(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.TYPESETTING_TABLE.get());
        var table = (TypesettingTableBlockEntity) helper.getBlockEntity(MACHINE);
        Player author = player(helper);
        var book = writtenBook(author);
        author.setItemInHand(InteractionHand.MAIN_HAND, book.copy());
        helper.useBlock(MACHINE, author, hit(table.getBlockPos(), Direction.NORTH, 0.77, 0.75));
        helper.assertTrue(author.getMainHandItem().isEmpty()
                        && ItemStack.matches(book, table.getItem(TypesettingTableBlockEntity.SOURCE)),
                "The original left-hand book mesh should align with the source interaction");

        author.setShiftKeyDown(true);
        helper.useBlock(MACHINE, author, hit(table.getBlockPos(), Direction.NORTH, 0.77, 0.75));
        author.setShiftKeyDown(false);
        var library = PrintingLibrary.get(helper.getLevel());
        var entry = library.publish(book, author);
        table.select(entry.id());
        helper.assertTrue(table.getUpdateTag().getInt("VisibleParts") == 2 && !table.getUpdateTag().contains("Items"),
                "Saving should swap the red book for the blue mesh without broadcasting the contents");
        author.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIBLIO_CHASE.get(), 18));
        helper.useBlock(MACHINE, author, hit(table.getBlockPos(), Direction.NORTH, 0.22, 0.5));
        helper.assertTrue(author.getMainHandItem().isEmpty() && table.getUpdateTag().getInt("VisibleParts") == 26,
                "The chase well should show two authored layers for 18 chases");

        helper.setBlock(MACHINE.west(), Blocks.REDSTONE_BLOCK);
        sample(helper, table);
        var plate = table.removeItem(TypesettingTableBlockEntity.RESULT, 1);
        helper.assertTrue(entry.id().equals(PrintingPlateItem.bookId(plate))
                        && table.getItem(TypesettingTableBlockEntity.CHASES).getCount() == 17,
                "A powered table should convert exactly one chase into a plate for its selection");
        var restored = (TypesettingTableBlockEntity) BlockEntity.loadStatic(table.getBlockPos(),
                table.getBlockState(), table.saveWithFullMetadata());
        restored.setLevel(helper.getLevel());
        sample(helper, restored);
        helper.assertTrue(restored.getItem(TypesettingTableBlockEntity.RESULT).isEmpty(),
                "Held power across reload should never stamp a second plate");

        table.setItem(TypesettingTableBlockEntity.SOURCE, plate);
        helper.assertTrue(table.processSource(author)
                        && table.getItem(TypesettingTableBlockEntity.CHASES).getCount() == 18,
                "Recycling a standard plate should recover its chase");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void printingLibraryKeepsPermissionsAndFullBookSnapshots(GameTestHelper helper) {
        Player author = player(helper);
        Player guest = player(helper);
        var library = new PrintingLibrary();
        var written = writtenBook(author);
        var big = signed(ModItems.BIG_BOOK.get().getDefaultInstance(), author);
        big.getOrCreateTag().putInt("pagesCurrent", 42);
        var notes = new CompoundTag();
        notes.putString("entry", "§4Preserve these notes exactly");
        big.getOrCreateTag().put("chapters", notes);
        var recipe = signed(ModItems.RECIPE_BOOK.get().getDefaultInstance(), author);
        recipe.getOrCreateTag().putString("text", "crafting notes");
        var catalog = new ItemStack(ModItems.STOCKROOM_CATALOG.get());
        catalog.getOrCreateTag().put("TestCatalogData", notes.copy());

        for (ItemStack source : List.of(written, big, recipe, catalog)) {
            var entry = library.publish(source, author);
            helper.assertTrue(entry != null && library.publish(source, author).id().equals(entry.id())
                            && !library.toggleVisibility(entry.id(), guest) && !library.delete(entry.id(), guest),
                    "Re-publishing identical data should reuse its entry and guests cannot modify it");
            boolean shared = BiblioConfig.COMMON.enablePublicTypesettingBooks.get();
            helper.assertTrue(entry.canRead(guest) == shared && library.toggleVisibility(entry.id(), author)
                            && library.find(entry.id()).canRead(guest) != shared,
                    "Only the author should control a book's default visibility and subsequent sharing");
            var saved = PrintingLibrary.load(library.save(new CompoundTag()));
            helper.assertTrue(ItemStack.matches(source, saved.find(entry.id()).book()),
                    "The world library must round-trip the complete item data");
        }
        var first = library.visibleTo(author).get(0);
        var row = new TypesettingTableMenu.Row(first.id(), first.title(), first.author(), first.shared(), true);
        var page = new TypesettingTableMenu.Page(1, 2, List.of(row), first.id());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            page.write(buffer);
            helper.assertTrue(page.equals(TypesettingTableMenu.Page.read(buffer)) && !buffer.isReadable(),
                    "Only bounded page metadata should cross the library menu protocol");
        } finally {
            buffer.release();
        }
        helper.assertTrue(library.delete(first.id(), author) && library.find(first.id()) == null,
                "Removing a library entry should invalidate its plate ID");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 230)
    public static void pressCompletesTheNormalTimedJobAndRetainsItsPlate(GameTestHelper helper) {
        var press = press(helper);
        var author = player(helper);
        var book = writtenBook(author);
        var entry = PrintingLibrary.get(helper.getLevel()).publish(book, author);
        var plate = PrintingPlateItem.forBook(entry);
        press.setItem(PrintingPressBlockEntity.PLATE, plate);
        press.setItem(PrintingPressBlockEntity.INK, new ItemStack(Items.BLACK_DYE, 8));
        press.setItem(PrintingPressBlockEntity.BLANK, new ItemStack(Items.BOOK, 34));
        helper.runAfterDelay(10, () -> {
            CompoundTag update = press.getUpdateTag();
            helper.assertTrue(update.contains("AnimationStart") && update.getInt("InkLevel") == 1
                            && update.getInt("VisibleParts") == 15 && !update.contains("Items"),
                    "An active press should sync animation, one ink band, three blank meshes, and no book contents");
            var restored = (PrintingPressBlockEntity) BlockEntity.loadStatic(press.getBlockPos(),
                    press.getBlockState(), press.saveWithFullMetadata());
            helper.assertTrue(restored.ticksLeft() == press.ticksLeft()
                            && restored.getItem(PrintingPressBlockEntity.INK).getCount() == 7
                            && ItemStack.matches(plate, restored.getItem(PrintingPressBlockEntity.PLATE)),
                    "The ink already spent and remaining print time must survive a chunk reload");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(ItemStack.matches(book, press.getItem(PrintingPressBlockEntity.RESULT))
                            && ItemStack.matches(plate, press.getItem(PrintingPressBlockEntity.PLATE))
                            && press.getItem(PrintingPressBlockEntity.BLANK).getCount() == 33
                            && press.getItem(PrintingPressBlockEntity.INK).getCount() == 7,
                    "One book must complete with one blank and one dye spent and the reusable plate retained");
        });
    }

    @GameTest(template = "empty")
    public static void enchantmentPlateChargesLevelsAndHasFinitePrints(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.TYPESETTING_TABLE.get());
        var table = (TypesettingTableBlockEntity) helper.getBlockEntity(MACHINE);
        var enchanted = new ItemStack(Items.ENCHANTED_BOOK);
        enchanted.setHoverName(Component.literal("My enchanted source"));
        EnchantedBookItem.addEnchantment(enchanted, new EnchantmentInstance(Enchantments.MENDING, 1));
        EnchantedBookItem.addEnchantment(enchanted, new EnchantmentInstance(Enchantments.UNBREAKING, 3));
        table.setItem(TypesettingTableBlockEntity.SOURCE, enchanted);
        table.setItem(TypesettingTableBlockEntity.CHASES, new ItemStack(ModItems.BIBLIO_CHASE.get(), 2));
        Player owner = player(helper);
        int cost = TypesettingTableBlockEntity.enchantmentCost(enchanted);
        helper.assertTrue(cost == (int) (60 * BiblioConfig.COMMON.enchantmentMultiplyer.get() / 10.0)
                        && !table.processSource(owner),
                "An under-levelled player must keep their inputs and the legacy enchantment formula must apply");
        owner.giveExperienceLevels(cost + 2);
        helper.assertTrue(table.processSource(owner) && owner.experienceLevel == 2
                        && table.getItem(TypesettingTableBlockEntity.SOURCE).isEmpty()
                        && table.getItem(TypesettingTableBlockEntity.CHASES).getCount() == 1,
                "Pressing a valid enchantment plate should charge levels and consume the original book and chase");
        var plate = table.removeItem(TypesettingTableBlockEntity.RESULT, 1);
        int uses = PrintingPlateItem.remainingUses(plate);
        var press = press(helper);
        press.setItem(PrintingPressBlockEntity.PLATE, plate);
        press.setItem(PrintingPressBlockEntity.BLANK, new ItemStack(Items.BOOK, uses));
        press.setItem(PrintingPressBlockEntity.INK, new ItemStack(Items.INK_SAC, uses));
        for (int copy = 0; copy < uses; copy++) {
            for (int tick = 0; tick <= PrintingPressBlockEntity.PRINT_TICKS; tick++) {
                press.tick(helper.getLevel());
            }
            helper.assertTrue(ItemStack.matches(enchanted, press.removeItem(PrintingPressBlockEntity.RESULT, 1)),
                    "Each print should preserve both enchantments and their levels");
        }
        helper.assertTrue(press.isEmpty(), "Successful copies should break the plate at its configured use count");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pressFiltersSlotsRespectsLocksAndDropsContents(GameTestHelper helper) {
        var press = press(helper);
        var player = player(helper);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var state = press.getBlockState().setValue(HorizontalFacingEntityBlock.FACING, facing);
            helper.assertTrue(PrintingPressBlock.slotAt(state, hit(press.getBlockPos(), facing, 0.15, 0.5)) == 0
                            && PrintingPressBlock.slotAt(state, hit(press.getBlockPos(), facing, 0.44, 0.5)) == 1
                            && PrintingPressBlock.slotAt(state, hit(press.getBlockPos(), facing, 0.83, 0.8)) == 2
                            && PrintingPressBlock.slotAt(state, hit(press.getBlockPos(), facing, 0.83, 0.2)) == 3,
                    "The four input and output regions should rotate with their authored meshes");
            var tableState = ModBlocks.TYPESETTING_TABLE.get().defaultBlockState()
                    .setValue(HorizontalFacingEntityBlock.FACING, facing);
            helper.assertTrue(TypesettingTableBlock.slotAt(tableState, hit(press.getBlockPos(), facing, 0.77, 0.75)) == 0
                            && TypesettingTableBlock.slotAt(tableState, hit(press.getBlockPos(), facing, 0.22, 0.5)) == 1
                            && TypesettingTableBlock.slotAt(tableState, hit(press.getBlockPos(), facing, 0.77, 0.25)) == 2,
                    "The source, chase, and plate regions should rotate with the original table");
        }
        var menu = new PrintingPressMenu(1, player.getInventory(), press);
        player.getInventory().setItem(9, new ItemStack(Items.BOOK, 3));
        helper.assertTrue(menu.quickMoveStack(player, 4).getCount() == 3
                        && press.getItem(PrintingPressBlockEntity.BLANK).getCount() == 3
                        && !menu.getSlot(PrintingPressBlockEntity.RESULT).mayPlace(new ItemStack(Items.BOOK)),
                "Shift-click should feed the blank slot and keep the output slot extraction-only");
        var top = press.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(IllegalStateException::new);
        var bottom = press.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(IllegalStateException::new);
        helper.assertTrue(top.getSlots() == 3 && bottom.getSlots() == 1
                        && top.insertItem(PrintingPressBlockEntity.INK, new ItemStack(Items.RED_DYE), true).is(Items.RED_DYE)
                        && top.extractItem(PrintingPressBlockEntity.BLANK, 1, true).isEmpty(),
                "The intake should reject other dyes and only accept insertion");
        press.setItem(PrintingPressBlockEntity.RESULT, writtenBook(player));
        press.toggleLock(player);
        helper.assertTrue(bottom.extractItem(0, 1, false).isEmpty()
                        && !top.insertItem(PrintingPressBlockEntity.INK, new ItemStack(Items.BLACK_DYE), false).isEmpty(),
                "A locked press should reject capability access from both ends");
        press.toggleLock(player);
        helper.assertTrue(bottom.extractItem(0, 1, true).is(Items.WRITTEN_BOOK), "The lower hopper can collect print output");
        helper.getLevel().destroyBlock(press.getBlockPos(), true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.BOOK, MACHINE, 2, 3);
            helper.assertItemEntityCountIs(Items.WRITTEN_BOOK, MACHINE, 2, 1);
            helper.assertItemEntityCountIs(ModBlocks.PRINTING_PRESS.get().asItem(), MACHINE, 2, 1);
        });
    }

    private static PrintingPressBlockEntity press(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.PRINTING_PRESS.get());
        return (PrintingPressBlockEntity) helper.getBlockEntity(MACHINE);
    }

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(MACHINE)).add(1, 0, 0));
        return player;
    }

    private static ItemStack writtenBook(Player author) {
        var book = signed(new ItemStack(Items.WRITTEN_BOOK), author);
        var pages = new ListTag();
        pages.add(StringTag.valueOf("{\"text\":\"Original text\"}"));
        book.getOrCreateTag().put("pages", pages);
        return book;
    }

    private static ItemStack signed(ItemStack book, Player author) {
        var tag = book.getOrCreateTag();
        tag.putString("title", "Printing Test");
        tag.putString("author", author.getGameProfile().getName());
        tag.putBoolean("signed", true);
        return book;
    }

    private static BlockHitResult hit(BlockPos pos, Direction facing, double x, double z) {
        Vec3 relative = switch (facing) {
            case EAST -> new Vec3(1 - z, 0.92, x);
            case SOUTH -> new Vec3(1 - x, 0.92, 1 - z);
            case WEST -> new Vec3(z, 0.92, 1 - x);
            default -> new Vec3(x, 0.92, z);
        };
        return new BlockHitResult(relative.add(Vec3.atLowerCornerOf(pos)), Direction.UP, pos, false);
    }

    private static void sample(GameTestHelper helper, TypesettingTableBlockEntity table) {
        for (int tick = 0; tick < 3; tick++) {
            table.tick(helper.getLevel());
        }
    }
}
