package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.DeskBlock;
import art.arcane.bibliocraft.block.DeskSection;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import art.arcane.bibliocraft.blockentity.TypewriterBlockEntity;
import art.arcane.bibliocraft.menu.DeskBookMenu;
import art.arcane.bibliocraft.menu.DeskMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.storygen.StoryBook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class DeskGameTests {
    private static final BlockPos DESK = new BlockPos(2, 1, 2);

    private DeskGameTests() {}

    @GameTest(template = "empty")
    public static void deskHitTargetsTrackTheAuthoredBookStacks(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            helper.setBlock(DESK, ModBlocks.DESK.get().defaultBlockState().setValue(DeskBlock.FACING, facing));
            var desk = (DeskBlockEntity) helper.getBlockEntity(DESK);
            desk.clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK));
            helper.useBlock(DESK, player, top(desk, 0.2, 0.75));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ENCHANTED_BOOK));
            helper.useBlock(DESK, player, top(desk, 0.2, 0.25));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WRITABLE_BOOK));
            helper.useBlock(DESK, player, top(desk, 0.75, 0.5));
            helper.assertTrue(desk.getItem(DeskBlockEntity.LEFT_BOOKS).is(Items.BOOK)
                            && desk.getItem(DeskBlockEntity.RIGHT_BOOKS).is(Items.ENCHANTED_BOOK)
                            && desk.getItem(DeskBlockEntity.OPEN_BOOK).is(Items.WRITABLE_BOOK),
                    "The clicked surface must track the book geometry in every rotation");
        }
        var desk = (DeskBlockEntity) helper.getBlockEntity(DESK);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_CARPET, 3));
        helper.useBlock(DESK, player, hit(desk.getBlockPos(), Direction.EAST));
        helper.assertTrue(player.getMainHandItem().getCount() == 2 && desk.getItem(DeskBlockEntity.CARPET).getCount() == 1
                        && desk.getModelData().get(DeskBlockEntity.CARPET_COLOR) == DyeColor.RED,
                "A side click should use exactly one carpet and expose its color to the model");
        player.setPos(Vec3.atCenterOf(desk.getBlockPos()));
        var menu = new DeskMenu(1, player.getInventory(), desk);
        helper.assertTrue(menu.slots.size() == 45 && menu.stillValid(player) && !menu.slots.get(0).mayPlace(new ItemStack(Items.STONE)),
                "The legacy menu should expose nine filtered book slots and thirty-six player slots");
        desk.setItem(DeskBlockEntity.OPEN_BOOK, new ItemStack(Items.FILLED_MAP));
        helper.assertTrue((desk.visibleParts() & (1 << 8)) == 0 && desk.getItem(DeskBlockEntity.OPEN_BOOK).is(Items.FILLED_MAP),
                "A filled map should stay on the desk and hide the open-book mesh");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deskJoinsFormWritableRowsAndDetachWhenBroken(GameTestHelper helper) {
        var state = ModBlocks.DESK.get().defaultBlockState().setValue(DeskBlock.FACING, Direction.WEST);
        BlockPos left = DESK.east();
        BlockPos right = DESK.west();
        for (BlockPos pos : List.of(left, DESK, right)) {
            helper.setBlock(pos, state);
        }
        Player player = helper.makeMockSurvivalPlayer();
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HAND_DRILL.get()));
        drill(helper, player, left);
        drill(helper, player, DESK);
        drill(helper, player, DESK);
        drill(helper, player, right);
        helper.assertBlockProperty(left, DeskBlock.SECTION, DeskSection.LEFT);
        helper.assertBlockProperty(DESK, DeskBlock.SECTION, DeskSection.CENTER);
        helper.assertBlockProperty(right, DeskBlock.SECTION, DeskSection.RIGHT);

        var desk = (DeskBlockEntity) helper.getBlockEntity(DESK);
        desk.setItem(DeskBlockEntity.LEFT_BOOKS, new ItemStack(Items.BOOK, 3));
        helper.getLevel().destroyBlock(desk.getBlockPos(), true);
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(left, DeskBlock.SECTION, DeskSection.SINGLE);
            helper.assertBlockProperty(right, DeskBlock.SECTION, DeskSection.SINGLE);
            helper.assertItemEntityCountIs(Items.BOOK, DESK, 2, 3);
            helper.assertItemEntityCountIs(ModBlocks.DESK.get().asItem(), DESK, 2, 1);
        });
    }

    @GameTest(template = "empty")
    public static void deskEditsAndSignsOnlyTheBookInTheActiveSession(GameTestHelper helper) {
        helper.setBlock(DESK, ModBlocks.DESK.get());
        var desk = (DeskBlockEntity) helper.getBlockEntity(DESK);
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(desk.getBlockPos()));
        desk.setItem(DeskBlockEntity.OPEN_BOOK, new ItemStack(Items.WRITABLE_BOOK));
        var draft = new DeskBookMenu(1, desk);
        player.containerMenu = draft;
        helper.assertTrue(draft.save(player, List.of("First page", "Second page"), Optional.empty(), 1),
                "A writable book should save in place");
        helper.assertTrue(desk.getPage() == 1 && !draft.stillValid(player)
                        && !draft.save(player, List.of("Stale"), Optional.of("Stale"), 0),
                "A stale edit session must not overwrite the current book");

        var signature = new DeskBookMenu(2, desk);
        player.containerMenu = signature;
        helper.assertTrue(signature.save(player, List.of("First page", "Second page"), Optional.of("Oak Journal"), 1),
                "Signing should keep all pages on the same desk");
        var saved = (DeskBlockEntity) BlockEntity.loadStatic(desk.getBlockPos(), desk.getBlockState(), desk.saveWithFullMetadata());
        ItemStack book = saved.getItem(DeskBlockEntity.OPEN_BOOK);
        helper.assertTrue(saved.getPage() == 1 && book.is(Items.WRITTEN_BOOK) && WrittenBookItem.makeSureTagIsValid(book.getTag())
                        && book.getTag().getList("pages", Tag.TAG_STRING).getString(1).contains("Second page")
                        && book.getTag().getString("author").equals(player.getGameProfile().getName()),
                "The signed book and remembered page should survive reload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void typewriterDeliversBooksLeftThenRightWithoutLosingOverflow(GameTestHelper helper) {
        helper.setBlock(DESK, ModBlocks.DESK.get());
        helper.setBlock(DESK.above(), ModBlocks.TYPEWRITER.get());
        var desk = (DeskBlockEntity) helper.getBlockEntity(DESK);
        var writer = (TypewriterBlockEntity) helper.getBlockEntity(DESK.above());
        ItemStack book = StoryBook.create(helper.getLevel().random);
        Player player = helper.makeMockSurvivalPlayer();
        desk.toggleLock(player);
        writer.setItem(TypewriterBlockEntity.BOOK, book.copy());
        typewriterStep(writer);
        helper.assertTrue(desk.isEmpty() && ItemStack.matches(writer.getItem(TypewriterBlockEntity.BOOK), book),
                "A locked desk must leave the signed book in the typewriter");
        desk.toggleLock(player);

        for (int slot = DeskBlockEntity.LEFT_BOOKS; slot < DeskBlockEntity.CARPET; slot++) {
            writer.setItem(TypewriterBlockEntity.BOOK, book.copy());
            typewriterStep(writer);
            helper.assertTrue(writer.getItem(TypewriterBlockEntity.BOOK).isEmpty() && ItemStack.matches(desk.getItem(slot), book),
                    "The open desk should take the book into the next stack slot");
        }
        writer.setItem(TypewriterBlockEntity.BOOK, book.copy());
        typewriterStep(writer);
        helper.assertTrue(ItemStack.matches(writer.getItem(TypewriterBlockEntity.BOOK), book)
                        && desk.getItem(DeskBlockEntity.OPEN_BOOK).isEmpty(),
                "Overflow should remain in the machine with the writing surface free");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deskCraftingAndLootRetainEveryFurnitureMaterial(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        grid.setItem(0, new ItemStack(Items.TORCH));
        grid.setItem(2, new ItemStack(Items.FEATHER));
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            var slab = wood == FurnitureMaterial.Wood.FRAMED ? ModItems.FRAMING_BOARD.get()
                    : ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
            var plank = wood == FurnitureMaterial.Wood.FRAMED ? ModItems.FRAMING_SHEET.get()
                    : ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(wood.id() + "_planks"));
            for (int slot : new int[]{3, 4, 5}) {
                grid.setItem(slot, new ItemStack(slab));
            }
            grid.setItem(6, new ItemStack(plank));
            grid.setItem(8, new ItemStack(plank));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack crafted = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(crafted.is(ModBlocks.DESK.get().asItem()) && FurnitureMaterial.fromItem(crafted).wood() == wood,
                    "Matching parts should craft the original writing desk pattern and material");
        }
        helper.setBlock(DESK.below(), Blocks.STONE);
        BlockPos pos = helper.absolutePos(DESK);
        var brick = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("bricks"));
        var framed = brick.applyTo(new ItemStack(ModBlocks.DESK.get()));
        player.setItemInHand(InteractionHand.MAIN_HAND, framed);
        framed.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos.below(), Direction.UP)));
        var desk = (DeskBlockEntity) helper.getBlockEntity(DESK);
        helper.assertTrue(brick.equals(desk.getMaterial()), "A framed desk should apply its saved panel on ordinary placement");
        level.destroyBlock(pos, true);
        helper.succeedWhen(() -> helper.assertTrue(helper.getEntities(EntityType.ITEM, DESK, 2).stream()
                        .anyMatch(item -> item.getItem().is(ModBlocks.DESK.get().asItem()) && brick.equals(FurnitureMaterial.fromItem(item.getItem()))),
                "Breaking should drop the paneled desk with its saved brick material"));
    }

    private static void drill(GameTestHelper helper, Player player, BlockPos relative) {
        player.getMainHandItem().onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND,
                hit(helper.absolutePos(relative), Direction.UP)));
    }

    private static void typewriterStep(TypewriterBlockEntity writer) {
        for (int tick = 0; tick < TypewriterBlockEntity.AUTOMATIC_INTERVAL; tick++) {
            writer.tick();
        }
    }

    private static BlockHitResult hit(BlockPos pos, Direction direction) {
        return new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0, 1, 0), direction, pos, false);
    }

    private static BlockHitResult top(DeskBlockEntity desk, double x, double z) {
        float yaw = (180 - desk.getBlockState().getValue(DeskBlock.FACING).toYRot()) * Mth.DEG_TO_RAD;
        BlockPos pos = desk.getBlockPos();
        Vec3 target = Vec3.atCenterOf(pos).add(new Vec3(x - 0.5, 0.5, z - 0.5).yRot(yaw));
        return new BlockHitResult(target, Direction.UP, pos, false);
    }
}
