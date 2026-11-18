package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.BookcaseBlock;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.blockentity.BookcaseBlockEntity;
import art.arcane.bibliocraft.menu.BookcaseMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class BookcaseGameTests {
    private static final BlockPos CASE = new BlockPos(2, 1, 2);

    private BookcaseGameTests() {}

    @GameTest(template = "empty")
    public static void bookcaseClicksMatchTheSixteenAuthoredSlots(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        BlockPos pos = helper.absolutePos(CASE);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            helper.setBlock(CASE, ModBlocks.BOOKCASE.get().defaultBlockState().setValue(BookcaseBlock.FACING, facing));
            BookcaseBlockEntity bookcase = (BookcaseBlockEntity) helper.getBlockEntity(CASE);
            Direction opening = facing.getClockWise();
            for (int slot = 0; slot < BookcaseBlockEntity.SLOT_COUNT; slot++) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK, slot + 1));
                helper.useBlock(CASE, player, hitAt(helper, pos, opening, slot));
                helper.assertTrue(bookcase.getItem(slot).getCount() == slot + 1 && player.getMainHandItem().isEmpty(),
                        "A front click in " + opening + " should insert into slot " + slot);
            }
            helper.assertTrue(bookcase.getOccupiedSlots() == 0xFFFF
                            && bookcase.getBlockState().getEnchantPowerBonus(helper.getLevel(), pos) == 2.0F,
                    "Each occupied slot should contribute one eighth of a bookshelf");
            bookcase.clearContent();
        }

        helper.setBlock(CASE, ModBlocks.BOOKCASE.get().defaultBlockState().setValue(BookcaseBlock.FACING, Direction.NORTH));
        BookcaseBlockEntity bookcase = (BookcaseBlockEntity) helper.getBlockEntity(CASE);
        bookcase.setItem(15, new ItemStack(Items.BOOK, 7));
        player.setShiftKeyDown(true);
        helper.useBlock(CASE, player, hitAt(helper, pos, Direction.EAST, 15));
        helper.assertTrue(bookcase.getItem(15).isEmpty(), "Sneaking should remove the selected stack");
        helper.succeedWhen(() -> helper.assertItemEntityCountIs(Items.BOOK, CASE, 2, 7));
    }

    @GameTest(template = "empty")
    public static void bookcaseSavesAndSyncsOccupancyAndFiltersTheMenu(GameTestHelper helper) {
        helper.setBlock(CASE, ModBlocks.BOOKCASE.get());
        var bookcase = (BookcaseBlockEntity) helper.getBlockEntity(CASE);
        var material = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED,
                ResourceLocation.withDefaultNamespace("bricks"));
        CompoundTag appearance = new CompoundTag();
        material.save(appearance);
        bookcase.load(appearance);
        Player player = helper.makeMockSurvivalPlayer();
        BookcaseMenu menu = new BookcaseMenu(1, player.getInventory(), bookcase);
        int firstPlayerSlot = BookcaseBlockEntity.SLOT_COUNT;
        player.getInventory().setItem(9, new ItemStack(Items.DIRT).setHoverName(Component.literal("Book")));
        helper.assertTrue(!menu.getSlot(0).mayPlace(player.getInventory().getItem(9))
                        && menu.quickMoveStack(player, firstPlayerSlot).isEmpty(),
                "Renaming dirt should not make it a book");
        player.getInventory().setItem(9, new ItemStack(Items.BOOK, 3));
        menu.quickMoveStack(player, firstPlayerSlot);
        bookcase.setItem(9, new ItemStack(Items.MAP));
        helper.assertTrue(bookcase.getItem(0).getCount() == 3 && player.getInventory().getItem(9).isEmpty(),
                "Shift-click should move books from the player to the case");
        menu.quickMoveStack(player, 0);
        helper.assertTrue(bookcase.getItem(0).isEmpty() && player.getInventory().countItem(Items.BOOK) == 3,
                "Shift-click should return the whole stack to the player");

        var loaded = (BookcaseBlockEntity) BlockEntity.loadStatic(bookcase.getBlockPos(),
                bookcase.getBlockState(), bookcase.saveWithFullMetadata());
        helper.assertTrue(loaded != null && material.equals(loaded.getMaterial()) && loaded.getItem(9).is(Items.MAP),
                "A saved case should retain its panel and the book's slot");
        var rendered = new BookcaseBlockEntity(bookcase.getBlockPos(), bookcase.getBlockState());
        rendered.handleUpdateTag(bookcase.getUpdateTag());
        helper.assertTrue(rendered.getModelData().get(BookcaseBlockEntity.OCCUPIED_SLOTS) == 1 << 9,
                "The update tag should reveal only mesh book10");
        rendered.handleUpdateTag(new CompoundTag());
        helper.assertTrue(rendered.getModelData().get(BookcaseBlockEntity.OCCUPIED_SLOTS) == 0,
                "An empty update should hide every book mesh");

        BlockPos pos = bookcase.getBlockPos();
        var clone = bookcase.getBlockState().getBlock()
                .getCloneItemStack(helper.getLevel(), pos, bookcase.getBlockState());
        helper.assertTrue(material.equals(FurnitureMaterial.fromItem(clone))
                        && !BlockItem.getBlockEntityData(clone).contains("Items"),
                "Pick-block should copy only the panel");
        helper.getLevel().destroyBlock(pos, true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.MAP, CASE, 2, 1);
            helper.assertItemEntityCountIs(ModBlocks.BOOKCASE.get().asItem(), CASE, 2, 1);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                            .map(ItemEntity::getItem).anyMatch(drop -> drop.is(ModBlocks.BOOKCASE.get().asItem())
                                    && material.equals(FurnitureMaterial.fromItem(drop))
                                    && !BlockItem.getBlockEntityData(drop).contains("Items")),
                    "The bookcase drop should retain the panel and leave its contents as separate items");
        });
    }

    @GameTest(template = "empty")
    public static void bookcaseRedstoneUsesTheFirstSlotAndPowersItsNeighbors(GameTestHelper helper) {
        helper.setBlock(CASE, ModBlocks.BOOKCASE.get().defaultBlockState().setValue(BookcaseBlock.FACING, Direction.NORTH));
        helper.setBlock(CASE.east(), Blocks.REDSTONE_LAMP);
        helper.setBlock(CASE.west(), Blocks.STONE);
        helper.setBlock(CASE.west(2), Blocks.REDSTONE_LAMP);
        BookcaseBlockEntity bookcase = (BookcaseBlockEntity) helper.getBlockEntity(CASE);
        bookcase.setItem(13, new ItemStack(ModItems.BIBLIO_RED_BOOK.get()));
        helper.assertTrue(helper.getLevel().getSignal(bookcase.getBlockPos(), Direction.EAST) == 13,
                "Slot 13 should output signal 13 behind the bookcase");
        helper.assertBlockProperty(CASE.east(), RedstoneLampBlock.LIT, false);
        helper.assertBlockProperty(CASE.west(2), RedstoneLampBlock.LIT, true);
        bookcase.setItem(4, new ItemStack(ModItems.BIBLIO_RED_BOOK.get()));
        helper.assertTrue(helper.getLevel().getSignal(bookcase.getBlockPos(), Direction.EAST) == 4,
                "The first redstone book should determine the signal");
        bookcase.clearContent();
        helper.succeedWhen(() -> helper.assertBlockProperty(CASE.west(2), RedstoneLampBlock.LIT, false));
    }

    @GameTest(template = "empty")
    public static void creativeBookcasePlacementGeneratesReadableVanillaBooks(GameTestHelper helper) {
        helper.setBlock(CASE.below(), Blocks.STONE);
        BlockPos pos = helper.absolutePos(CASE);
        Player player = helper.makeMockSurvivalPlayer();
        player.setYRot(90);
        BlockItem item = (BlockItem) ModBlocks.BOOKCASE_CREATIVE.get().asItem();
        ItemStack stack = new FurnitureMaterial(FurnitureMaterial.Wood.CHERRY, null).applyTo(new ItemStack(item));
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
        helper.assertTrue(item.place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit)).consumesAction(),
                "The creative bookcase should place as a normal block item");
        helper.assertBlockProperty(CASE, BookcaseBlock.FACING, Direction.NORTH);
        BookcaseBlockEntity bookcase = (BookcaseBlockEntity) helper.getBlockEntity(CASE);
        helper.assertTrue(bookcase.getMaterial().wood() == FurnitureMaterial.Wood.CHERRY && !bookcase.isEmpty(),
                "A creative bookcase should keep its wood and stock itself when placed");
        for (int slot = 0; slot < bookcase.getContainerSize(); slot++) {
            ItemStack book = bookcase.getItem(slot);
            if (!book.isEmpty()) {
                CompoundTag tag = book.getTag();
                helper.assertTrue(book.is(Items.WRITTEN_BOOK) && WrittenBookItem.makeSureTagIsValid(tag),
                        "Generated books should open in the vanilla reader");
                var pages = tag.getList("pages", Tag.TAG_STRING);
                helper.assertTrue(pages.size() == 8, "Every story should have eight pages");
                for (int page = 0; page < pages.size(); page++) {
                    helper.assertTrue(!Component.Serializer.fromJson(pages.getString(page)).getString().isBlank(),
                            "Each story page should contain readable text");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bookcaseRecipesCarryWoodAndFrameMaterials(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            boolean framed = wood == FurnitureMaterial.Wood.FRAMED;
            var plank = framed ? ModItems.FRAMING_SHEET.get()
                    : BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(wood.id() + "_planks"));
            var slab = framed ? ModItems.FRAMING_BOARD.get()
                    : BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
            for (int slot = 0; slot < 9; slot++) {
                grid.setItem(slot, new ItemStack(slot % 3 == 1 ? slab : plank));
            }
            var level = helper.getLevel();
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack result = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(result.is(ModBlocks.BOOKCASE.get().asItem())
                            && FurnitureMaterial.fromItem(result).wood() == wood,
                    "Matching " + wood.id() + " materials should craft the corresponding bookcase");
        }
        helper.succeed();
    }

    private static BlockHitResult hitAt(GameTestHelper helper, BlockPos pos, Direction opening, int slot) {
        double height = slot < BookcaseBlockEntity.COLUMNS ? 0.25 : -0.25;
        double offset = (slot % BookcaseBlockEntity.COLUMNS + 0.5) / BookcaseBlockEntity.COLUMNS - 0.5;
        Vec3 location = Vec3.atCenterOf(pos).add(0, height, 0).relative(opening.getCounterClockWise(), offset);
        BlockHitResult hit = helper.getBlockState(CASE).getShape(helper.getLevel(), pos)
                .clip(location.relative(opening, 2), location.relative(opening, -1), pos);
        helper.assertTrue(hit != null && hit.getDirection() == opening, "The bookcase should open " + opening);
        return hit;
    }
}
