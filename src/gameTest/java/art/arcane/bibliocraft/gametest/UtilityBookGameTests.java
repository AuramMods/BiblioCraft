package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.BookcaseBlock;
import art.arcane.bibliocraft.blockentity.BookcaseBlockEntity;
import art.arcane.bibliocraft.book.SlottedBookContents;
import art.arcane.bibliocraft.book.SlottedBookContents.Label;
import art.arcane.bibliocraft.menu.RedstoneBookMenu;
import art.arcane.bibliocraft.menu.SlottedBookMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class UtilityBookGameTests {
    private static final BlockPos CASE = new BlockPos(2, 1, 2);

    private UtilityBookGameTests() {}

    @GameTest(template = "empty")
    public static void slottedBookMovesAndSavesRealItemsBeforeTheMenuCloses(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var inventory = player.getInventory();
        inventory.selected = 2;
        var book = new ItemStack(ModItems.SLOTTED_BOOK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        var input = new ItemStack(Items.DIAMOND_PICKAXE).setHoverName(Component.literal("Spare pick"));
        input.setDamageValue(19);
        inventory.setItem(9, input.copy());

        var menu = new SlottedBookMenu(1, inventory, InteractionHand.MAIN_HAND);
        helper.assertTrue(ItemStack.matches(menu.quickMoveStack(player, 1), input)
                        && inventory.getItem(9).isEmpty(), "Shift-click should move the item into the book");
        var saved = ItemStack.of(book.save(new CompoundTag()));
        helper.assertTrue(ItemStack.matches(SlottedBookContents.stored(saved), input),
                "The item, name, and damage should save immediately");
        var label = new Label("Tool cache", List.of("Workshop", "Keep dry", "", "", "Spare kit"));
        menu.saveLabel(player, label);
        helper.assertTrue(SlottedBookContents.label(book).equals(label)
                        && ItemStack.matches(SlottedBookContents.stored(book), input),
                "Saving a label must preserve the inventory");

        var reopened = new SlottedBookMenu(2, inventory, InteractionHand.MAIN_HAND);
        helper.assertTrue(ItemStack.matches(reopened.quickMoveStack(player, 0), input)
                        && SlottedBookContents.stored(book).isEmpty() && inventory.countItem(Items.DIAMOND_PICKAXE) == 1,
                "Reopening and shift-clicking out should return exactly one copy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void slottedBookLocksItsHeldStackAndRejectsNestedBooks(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var inventory = player.getInventory();
        var book = new ItemStack(ModItems.SLOTTED_BOOK.get());
        var another = new ItemStack(ModItems.SLOTTED_BOOK.get());
        inventory.selected = 0;
        inventory.setItem(0, book);
        inventory.setItem(9, another);
        var menu = new SlottedBookMenu(1, inventory, InteractionHand.MAIN_HAND);
        helper.assertTrue(menu.quickMoveStack(player, 1).isEmpty() && !menu.getSlot(0).mayPlace(another),
                "The portable slot should reject another slotted book");
        menu.clicked(0, 0, ClickType.SWAP, player);
        menu.clicked(28, 0, ClickType.THROW, player);
        helper.assertTrue(inventory.getItem(0) == book && menu.getSlot(0).getItem().isEmpty()
                        && menu.quickMoveStack(player, 28).isEmpty(), "The open book should be locked in its hotbar slot");

        player.setItemInHand(InteractionHand.OFF_HAND, book.copy());
        var offhand = new SlottedBookMenu(2, inventory, InteractionHand.OFF_HAND);
        ItemStack original = player.getOffhandItem();
        offhand.clicked(1, 40, ClickType.SWAP, player);
        player.setItemInHand(InteractionHand.OFF_HAND, original.copy());
        offhand.saveLabel(player, new Label("Stale edit", List.of("", "", "", "", "")));
        helper.assertTrue(inventory.getItem(9) == another && !offhand.stillValid(player)
                        && !player.getOffhandItem().hasCustomHoverName() && !original.hasCustomHoverName(),
                "Offhand swaps and a replaced stack must never retarget the editor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void renamedRedstoneBookStillPowersItsBookcaseBySlot(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var book = new ItemStack(ModItems.BIBLIO_RED_BOOK.get());
        book.getOrCreateTag().putString("redstonebook", "by James Maxwell");
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        var menu = new RedstoneBookMenu(1, player, InteractionHand.MAIN_HAND);
        menu.rename(player, "  Workshop signal  ");
        helper.assertTrue(book.getHoverName().getString().equals("Workshop signal") && book.hasFoil()
                        && book.getTag().getString("redstonebook").equals("by James Maxwell"),
                "Renaming should preserve the item, author, and glint");
        var loaded = ItemStack.of(book.save(new CompoundTag()));
        player.setItemInHand(InteractionHand.MAIN_HAND, loaded);
        menu.rename(player, "Stale title");
        helper.assertTrue(book.getHoverName().getString().equals("Workshop signal")
                        && loaded.getHoverName().getString().equals("Workshop signal"),
                "The previous menu cannot rename either stack after replacement");

        helper.setBlock(CASE, ModBlocks.BOOKCASE.get().defaultBlockState().setValue(BookcaseBlock.FACING, Direction.NORTH));
        var bookcase = (BookcaseBlockEntity) helper.getBlockEntity(CASE);
        helper.assertTrue(bookcase.canPlaceItem(15, loaded), "The named redstone book should fit a bookcase");
        bookcase.setItem(15, loaded);
        var state = bookcase.getBlockState();
        bookcase.load(bookcase.saveWithoutMetadata());
        helper.assertTrue(state.getSignal(helper.getLevel(), bookcase.getBlockPos(), Direction.NORTH) == 15
                        && state.getSignal(helper.getLevel(), bookcase.getBlockPos(), Direction.WEST) == 0,
                "A saved book in slot sixteen should power every side except the opening");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bothUtilityBooksUseTheOriginalCraftingLayouts(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 2, 2);
        grid.setItem(0, new ItemStack(Items.WRITABLE_BOOK));
        grid.setItem(3, new ItemStack(ModBlocks.LABEL.get()));
        var slotted = craft(helper, grid);
        helper.assertTrue(slotted.is(ModItems.SLOTTED_BOOK.get()) && slotted.getMaxStackSize() == 1 && slotted.hasFoil(),
                "A label and quill book should craft a single glinting slotted book");
        grid.setItem(0, new ItemStack(Items.BOOK));
        grid.setItem(3, new ItemStack(Items.REDSTONE_TORCH));
        var redstone = craft(helper, grid);
        helper.assertTrue(redstone.is(ModItems.BIBLIO_RED_BOOK.get()) && redstone.hasFoil(),
                "A torch and ordinary book should craft a glinting redstone book");
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, TransientCraftingContainer grid) {
        var level = helper.getLevel();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
                .map(recipe -> recipe.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
    }
}
