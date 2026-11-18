package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.ColorFacingEntityBlock;
import art.arcane.bibliocraft.block.TypewriterBlock;
import art.arcane.bibliocraft.blockentity.TypewriterBlockEntity;
import art.arcane.bibliocraft.item.ColoredBlockItem;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class TypewriterGameTests {
    private static final BlockPos WRITER = new BlockPos(2, 1, 2);

    private TypewriterGameTests() {}

    @GameTest(template = "empty")
    public static void typewriterConservesPaperAndProducesAReadableSignedBook(GameTestHelper helper) {
        helper.setBlock(WRITER, ModBlocks.TYPEWRITER.get());
        var writer = (TypewriterBlockEntity) helper.getBlockEntity(WRITER);
        Player player = helper.makeMockSurvivalPlayer();
        Direction front = TypewriterBlock.front(writer.getBlockState());
        writer.setItem(TypewriterBlockEntity.PAPER, new ItemStack(Items.PAPER, 58));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER, 8));
        helper.useBlock(WRITER, player, hit(writer, front));
        helper.assertTrue(writer.getItem(TypewriterBlockEntity.PAPER).getCount() == 64 && player.getMainHandItem().getCount() == 2,
                "Insertion must conserve paper when there is only room for part of the stack");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(WRITER, player, hit(writer, front.getOpposite()));
        helper.assertTrue(writer.getProgress() == 0, "Empty-handed clicks behind the typewriter should not type");
        for (int progress = 1; progress <= TypewriterBlockEntity.LAST_LINE; progress++) {
            helper.useBlock(WRITER, player, hit(writer, front));
            helper.assertTrue(writer.getProgress() == progress && writer.getItem(TypewriterBlockEntity.BOOK).isEmpty(),
                    "Each keyboard click should advance one visual step without creating an early book");
        }
        var client = new TypewriterBlockEntity(writer.getBlockPos(), writer.getBlockState());
        client.handleUpdateTag(writer.getUpdateTag());
        helper.assertTrue(client.getProgress() == 15 && client.hasPaper(), "Update tags must carry the current paper pose");
        helper.useBlock(WRITER, player, hit(writer, front));
        assertBook(helper, writer.getItem(TypewriterBlockEntity.BOOK), player.getGameProfile().getName());
        helper.assertTrue(writer.getItem(TypewriterBlockEntity.PAPER).getCount() == 56,
                "Printing should take exactly eight sheets");

        helper.useBlock(WRITER, player, hit(writer, front));
        helper.assertTrue(writer.getProgress() == 0 && writer.getItem(TypewriterBlockEntity.BOOK).isEmpty(),
                "Collecting the book should reset the carriage and allow another book");
        player.setShiftKeyDown(true);
        helper.useBlock(WRITER, player, hit(writer, front));
        helper.assertTrue(!writer.hasPaper(), "Sneak-use should eject the remaining paper");
        helper.getLevel().destroyBlock(writer.getBlockPos(), true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.PAPER, WRITER, 2, 56);
            helper.assertItemEntityCountIs(Items.WRITTEN_BOOK, WRITER, 2, 1);
            helper.assertItemEntityCountIs(ModBlocks.TYPEWRITER.get().asItem(), WRITER, 2, 1);
        });
    }

    @GameTest(template = "empty")
    public static void namedMobWritesInItsOwnVoiceAfterTheTypewriterReloads(GameTestHelper helper) {
        helper.setBlock(WRITER, ModBlocks.TYPEWRITER.get());
        var writer = (TypewriterBlockEntity) helper.getBlockEntity(WRITER);
        writer.setItem(TypewriterBlockEntity.PAPER, new ItemStack(Items.PAPER, TypewriterBlockEntity.PAGE_COUNT));
        Direction front = TypewriterBlock.front(writer.getBlockState());
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, WRITER.relative(front.getOpposite()));
        pig.setCustomName(Component.literal("Scribbles"));
        autoStep(helper, writer);
        helper.assertTrue(writer.getProgress() == 0, "A named mob behind the machine should not type");

        Vec3 target = Vec3.atBottomCenterOf(writer.getBlockPos().relative(front));
        pig.setPos(target.x, target.y, target.z);
        for (int step = 0; step < TypewriterBlockEntity.LAST_LINE; step++) {
            autoStep(helper, writer);
        }
        helper.assertTrue(writer.getProgress() == 15 && writer.getItem(TypewriterBlockEntity.BOOK).isEmpty(),
                "The pig should have written all lines before the concluding automatic step");
        pig.discard();
        var saved = (TypewriterBlockEntity) BlockEntity.loadStatic(writer.getBlockPos(), writer.getBlockState(),
                writer.saveWithFullMetadata());
        helper.assertTrue(saved != null && saved.getProgress() == 15 && saved.hasPaper(),
                "The pending story and paper should survive saving at the last line");
        helper.getLevel().setBlockEntity(saved);
        autoStep(helper, saved);
        ItemStack book = saved.getItem(TypewriterBlockEntity.BOOK);
        assertBook(helper, book, "Scribbles");
        String first = book.getTag().getList("pages", Tag.TAG_STRING).getString(0);
        helper.assertTrue(first.contains("Oink") && !saved.hasPaper(),
                "The completed story should keep its pig vocabulary and consume the eight remaining sheets");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void typewriterSupportsLockedAutomationAndPreservesItsColorWhenBroken(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(WRITER);
        helper.setBlock(WRITER.below(), Blocks.STONE);
        Player player = helper.makeMockSurvivalPlayer();
        var stack = ColoredBlockItem.applyColor(new ItemStack(ModBlocks.TYPEWRITER.get()), DyeColor.PINK);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var placed = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, placed));
        helper.assertBlockProperty(WRITER, ColorFacingEntityBlock.COLOR, DyeColor.PINK);
        var writer = (TypewriterBlockEntity) helper.getBlockEntity(WRITER);
        helper.assertTrue(TypewriterBlock.front(writer.getBlockState()) == player.getDirection().getOpposite(),
                "Placement must turn the authored keyboard toward its player");

        var items = writer.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
        helper.assertTrue(items.insertItem(TypewriterBlockEntity.BOOK, new ItemStack(Items.PAPER), false).is(Items.PAPER),
                "The finished-book slot should reject automation input");
        helper.assertTrue(items.insertItem(TypewriterBlockEntity.PAPER, new ItemStack(Items.PAPER, 8), false).isEmpty(),
                "Pipes should be able to load the paper slot");
        writer.toggleLock(player);
        helper.assertTrue(items.extractItem(TypewriterBlockEntity.PAPER, 8, false).isEmpty(),
                "The shared furniture lock should protect paper from automation");

        Direction previous = writer.getBlockState().getValue(TypewriterBlock.FACING);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HAND_DRILL.get()));
        player.getMainHandItem().onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(writer, Direction.UP)));
        helper.assertBlockProperty(WRITER, TypewriterBlock.FACING, previous.getClockWise());
        helper.assertBlockProperty(WRITER, ColorFacingEntityBlock.COLOR, DyeColor.PINK);
        writer.toggleLock(player);
        writer.setItem(TypewriterBlockEntity.BOOK, new ItemStack(Items.WRITTEN_BOOK));
        level.destroyBlock(pos, true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.PAPER, WRITER, 2, 8);
            helper.assertItemEntityCountIs(Items.WRITTEN_BOOK, WRITER, 2, 1);
            helper.assertItemEntityCountIs(ModBlocks.TYPEWRITER.get().asItem(), WRITER, 2, 1);
            helper.assertTrue(helper.getEntities(EntityType.ITEM, WRITER, 2).stream().anyMatch(drop ->
                            drop.getItem().is(ModBlocks.TYPEWRITER.get().asItem()) && ColoredBlockItem.getColor(drop.getItem()) == DyeColor.PINK),
                    "Breaking must drop the dyed typewriter and both contents separately");
        });
    }

    @GameTest(template = "empty")
    public static void typewriterCraftsAndRecolorsAllSixteenShells(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        grid.setItem(0, new ItemStack(Items.IRON_INGOT));
        grid.setItem(1, new ItemStack(Items.PAPER));
        grid.setItem(2, new ItemStack(Items.IRON_INGOT));
        grid.setItem(3, new ItemStack(Items.IRON_BLOCK));
        grid.setItem(4, new ItemStack(Items.INK_SAC));
        grid.setItem(5, new ItemStack(Items.IRON_BLOCK));
        var dyes = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 2, 2);
        for (DyeColor color : DyeColor.values()) {
            var clay = ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(color.getSerializedName() + "_terracotta"));
            for (int slot = 6; slot < 9; slot++) {
                grid.setItem(slot, new ItemStack(clay));
            }
            var match = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack crafted = match.assemble(grid, level.registryAccess());
            helper.assertTrue(crafted.is(ModBlocks.TYPEWRITER.get().asItem()) && ColoredBlockItem.getColor(crafted) == color,
                    "Matching terracotta should produce the original " + color + " shell");

            var source = ColoredBlockItem.applyColor(new ItemStack(ModBlocks.TYPEWRITER.get()),
                    color == DyeColor.BLUE ? DyeColor.WHITE : DyeColor.BLUE);
            dyes.setItem(0, source);
            dyes.setItem(1, new ItemStack(DyeItem.byColor(color)));
            var recolor = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, dyes, level).orElseThrow();
            helper.assertTrue(ColoredBlockItem.getColor(recolor.assemble(dyes, level.registryAccess())) == color,
                    "Recoloring should store the selected shell color on the same item");
        }
        helper.succeed();
    }

    private static BlockHitResult hit(TypewriterBlockEntity writer, Direction face) {
        return new BlockHitResult(Vec3.atBottomCenterOf(writer.getBlockPos()).add(0, 0.08, 0),
                face, writer.getBlockPos(), false);
    }

    private static void autoStep(GameTestHelper helper, TypewriterBlockEntity writer) {
        var level = helper.getLevel();
        var ticker = writer.getBlockState().getTicker(level, ModBlockEntities.TYPEWRITER.get());
        for (int tick = 0; tick < TypewriterBlockEntity.AUTOMATIC_INTERVAL; tick++) {
            ticker.tick(level, writer.getBlockPos(), writer.getBlockState(), writer);
        }
    }

    private static void assertBook(GameTestHelper helper, ItemStack book, String author) {
        var tag = book.getTag();
        helper.assertTrue(book.is(Items.WRITTEN_BOOK) && tag != null && WrittenBookItem.makeSureTagIsValid(tag)
                        && tag.getString("author").equals(author) && tag.getList("pages", Tag.TAG_STRING).size() == 8,
                "The typewriter should produce a valid eight-page signed book by " + author);
    }
}
