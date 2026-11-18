package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.ColorFacingEntityBlock;
import art.arcane.bibliocraft.block.ColorMountedFacingEntityBlock;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.item.ColoredBlockItem;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class MountedLightGameTests {
    private static final BlockPos LIGHT = new BlockPos(2, 1, 2);

    private MountedLightGameTests() {}

    @GameTest(template = "empty")
    public static void mountedLightPreservesColorAcrossPlacementDrillingAndDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = helper.makeMockSurvivalPlayer();
        Player guest = helper.makeMockSurvivalPlayer();
        BlockPos pos = helper.absolutePos(LIGHT);
        helper.setBlock(LIGHT.below(), Blocks.STONE);
        for (Block block : lightBlocks()) {
            player.setXRot(65);
            player.setShiftKeyDown(false);
            ItemStack item = ColoredBlockItem.applyColor(new ItemStack(block), DyeColor.BLUE);
            player.setItemInHand(InteractionHand.MAIN_HAND, item);
            item.useOn(context(player, pos.below()));
            helper.assertBlockProperty(LIGHT, ColorFacingEntityBlock.COLOR, DyeColor.BLUE);
            helper.assertBlockProperty(LIGHT, ColorMountedFacingEntityBlock.FACE, AttachFace.FLOOR);
            var light = (FurnitureBlockEntity) helper.getBlockEntity(LIGHT);
            helper.assertTrue(light.getType() == ModBlockEntities.BIBLIOLIGHT.get(), "Mounted lights should keep their shared type");
            helper.assertTrue(light.getBlockState().getLightEmission(level, pos) == 15,
                    "All original light variants should emit full block light");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LIME_DYE, 2));
            helper.useBlock(LIGHT, player, hit(pos));
            helper.assertBlockProperty(LIGHT, ColorFacingEntityBlock.COLOR, DyeColor.LIME);
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "A live dye change should consume one dye");

            light.toggleLock(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HAND_DRILL.get()));
            guest.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HAND_DRILL.get()));
            drill(guest, pos);
            helper.assertBlockProperty(LIGHT, ColorMountedFacingEntityBlock.FACE, AttachFace.FLOOR);
            drill(player, pos);
            helper.assertBlockProperty(LIGHT, ColorMountedFacingEntityBlock.FACE, AttachFace.WALL);
            drill(player, pos);
            helper.assertBlockProperty(LIGHT, ColorMountedFacingEntityBlock.FACE, AttachFace.CEILING);
            drill(player, pos);
            helper.assertBlockProperty(LIGHT, ColorMountedFacingEntityBlock.FACE, AttachFace.FLOOR);

            Direction facing = light.getBlockState().getValue(ColorMountedFacingEntityBlock.FACING);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIBLIO_DRILL.get()));
            drill(player, pos);
            helper.assertBlockProperty(LIGHT, ColorMountedFacingEntityBlock.FACING, facing.getClockWise());
            helper.assertBlockProperty(LIGHT, ColorFacingEntityBlock.COLOR, DyeColor.LIME);
            var saved = (FurnitureBlockEntity) BlockEntity.loadStatic(pos, light.getBlockState(), light.saveWithFullMetadata());
            helper.assertTrue(saved != null && saved.canAccess(player) && !saved.canAccess(guest),
                    "Light lock ownership should survive a save");
            ItemStack pick = block.getCloneItemStack(level, pos, light.getBlockState());
            helper.assertTrue(pick.is(block.asItem()) && ColoredBlockItem.getColor(pick) == DyeColor.LIME,
                    "Pick-block should carry the metal and color");
            light.toggleLock(player);
            level.destroyBlock(pos, true);
            helper.assertItemEntityCountIs(block.asItem(), LIGHT, 2, 1);
            helper.assertTrue(helper.getEntities(EntityType.ITEM, LIGHT, 2).stream().map(ItemEntity::getItem)
                    .anyMatch(stack -> stack.is(block.asItem()) && ColoredBlockItem.getColor(stack) == DyeColor.LIME),
                    "The light drop should retain the dyed texture");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allFourLightItemsRecolorAndSyncUsingTheirOwnRecipes(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 2, 2);
        var level = helper.getLevel();
        for (Block block : lightBlocks()) {
            for (DyeColor color : DyeColor.values()) {
                DyeColor source = color == DyeColor.CYAN ? DyeColor.WHITE : DyeColor.CYAN;
                ItemStack light = ColoredBlockItem.applyColor(new ItemStack(block), source)
                        .setHoverName(Component.literal("Atrium Light"));
                grid.setItem(2, light);
                grid.setItem(1, new ItemStack(DyeItem.byColor(color)));
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
                ItemStack recolored = recipe.assemble(grid, level.registryAccess());
                helper.assertTrue(recolored.is(block.asItem()) && ColoredBlockItem.getColor(recolored) == color
                                && recolored.hasCustomHoverName() && ColoredBlockItem.getColor(light) == source,
                        "Recoloring must retain the lamp or lantern type, metal, and name");
                grid.setItem(2, recolored);
                helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(),
                        "A dye matching the current color should not be consumed");
            }
        }
        grid.setItem(2, new ItemStack(ModBlocks.LANTERN_IRON.get()));
        grid.setItem(1, new ItemStack(Items.ORANGE_DYE));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            @SuppressWarnings("unchecked")
            var serializer = (RecipeSerializer<net.minecraft.world.item.crafting.CraftingRecipe>) recipe.getSerializer();
            serializer.toNetwork(buffer, recipe);
            var remote = serializer.fromNetwork(recipe.getId(), buffer);
            helper.assertTrue(remote != null && remote.matches(grid, level)
                            && ColoredBlockItem.getColor(remote.assemble(grid, level.registryAccess())) == DyeColor.ORANGE,
                    "Recipe sync should retain the exact light item and category");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static List<Block> lightBlocks() {
        return List.of(ModBlocks.LAMP_GOLD.get(), ModBlocks.LAMP_IRON.get(),
                ModBlocks.LANTERN_GOLD.get(), ModBlocks.LANTERN_IRON.get());
    }

    private static void drill(Player player, BlockPos pos) {
        player.getMainHandItem().onItemUseFirst(context(player, pos));
    }

    private static UseOnContext context(Player player, BlockPos pos) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos));
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0, 1, 0), Direction.UP, pos, false);
    }
}
