package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.block.RackBlock;
import art.arcane.bibliocraft.block.RackType;
import art.arcane.bibliocraft.blockentity.RackBlockEntity;
import art.arcane.bibliocraft.menu.RackMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class RackGameTests {
    private static final BlockPos RACK = new BlockPos(2, 1, 2);

    private RackGameTests() {}

    @GameTest(template = "empty")
    public static void potionShelfSlotsAgreeFromBothSidesInEveryRotation(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        RackType type = RackType.POTIONS;
        Block block = ModBlocks.POTION_SHELF.get();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            helper.setBlock(RACK, block.defaultBlockState().setValue(RackBlock.FACING, facing));
            RackBlockEntity rack = (RackBlockEntity) helper.getBlockEntity(RACK);
            Direction opening = type.opening(facing);
            for (int slot = 0; slot < type.size(); slot++) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
                click(helper, player, type, slot, opening, opening);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 3));
                click(helper, player, type, slot, opening, opening.getOpposite());
                helper.assertTrue(rack.getItem(slot).is(Items.GLASS_BOTTLE) && rack.getItem(slot).getCount() == 5
                                && player.getMainHandItem().isEmpty(),
                        "The front and back should both reach potion slot " + slot + " in facing " + facing);
            }
            rack.clearContent();
        }
        RackBlockEntity rack = (RackBlockEntity) helper.getBlockEntity(RACK);
        Direction opening = type.opening(rack.getBlockState().getValue(RackBlock.FACING));
        ItemStack healing = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.HEALING);
        player.setItemInHand(InteractionHand.MAIN_HAND, healing);
        click(helper, player, type, 6, opening, opening);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.POISON));
        click(helper, player, type, 6, opening, opening);
        helper.assertTrue(PotionUtils.getPotion(rack.getItem(6)) == Potions.HEALING
                        && !player.getMainHandItem().isEmpty(),
                "Direct interaction should keep distinct potion contents separate");
        player.setShiftKeyDown(true);
        click(helper, player, type, 6, opening, opening.getOpposite());
        helper.assertTrue(rack.getItem(6).isEmpty() && PotionUtils.getPotion(player.getMainHandItem()) == Potions.POISON,
                "Sneaking from the rear should eject the stored potion and preserve the held one");
        helper.succeedWhen(() -> helper.assertItemEntityCountIs(Items.SPLASH_POTION, RACK, 2, 1));
    }

    @GameTest(template = "empty")
    public static void toolRackClickAndMenuRespectOccupiedSlotsAndToolTypes(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        Block block = ModBlocks.TOOL_RACK.get();
        RackType type = RackType.TOOLS;
        var tools = new ItemStack[]{new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.SHEARS),
                new ItemStack(Items.CROSSBOW), new ItemStack(Items.FISHING_ROD)};
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            helper.setBlock(RACK, block.defaultBlockState().setValue(RackBlock.FACING, facing));
            RackBlockEntity rack = (RackBlockEntity) helper.getBlockEntity(RACK);
            Direction opening = type.opening(facing);
            for (int slot = 0; slot < type.size(); slot++) {
                player.setItemInHand(InteractionHand.MAIN_HAND, tools[slot].copy());
                click(helper, player, type, slot, opening, opening);
                helper.assertTrue(ItemStack.matches(tools[slot], rack.getItem(slot)),
                        "A tool rack should keep the tool in the clicked slot " + slot + " for facing " + facing);
            }
            rack.clearContent();
        }

        RackBlockEntity rack = (RackBlockEntity) helper.getBlockEntity(RACK);
        RackMenu menu = new RackMenu(1, player.getInventory(), rack);
        player.getInventory().setItem(9, new ItemStack(Items.APPLE));
        helper.assertTrue(menu.quickMoveStack(player, type.size()).isEmpty(), "Apples should stay in the player inventory");
        player.getInventory().setItem(9, new ItemStack(Items.ARROW, 6));
        menu.quickMoveStack(player, type.size());
        helper.assertTrue(rack.getItem(0).is(Items.ARROW) && rack.getItem(0).getCount() == 6,
                "Arrows should shift-click into the first available tool slot");

        Direction opening = type.opening(rack.getBlockState().getValue(RackBlock.FACING));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        click(helper, player, type, 0, opening, opening);
        helper.assertTrue(rack.getItem(0).isEmpty(), "An empty-handed front click should retrieve the stored tools");
        helper.succeedWhen(() -> helper.assertItemEntityCountIs(Items.ARROW, RACK, 2, 6));
    }

    @GameTest(template = "empty")
    public static void potionRackSavesAndDropsPaneledInventory(GameTestHelper helper) {
        helper.setBlock(RACK, ModBlocks.POTION_SHELF.get());
        RackBlockEntity rack = (RackBlockEntity) helper.getBlockEntity(RACK);
        FurnitureMaterial material = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED,
                ResourceLocation.withDefaultNamespace("bricks"));
        CompoundTag appearance = new CompoundTag();
        material.save(appearance);
        rack.load(appearance);

        ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), Potions.LONG_FIRE_RESISTANCE);
        rack.setItem(11, potion.copy());
        var loaded = (RackBlockEntity) BlockEntity.loadStatic(rack.getBlockPos(), rack.getBlockState(), rack.saveWithFullMetadata());
        helper.assertTrue(loaded != null && ItemStack.matches(loaded.getItem(11), potion)
                        && material.equals(loaded.getMaterial()),
                "World save should preserve the potion's NBT and slot along with the panel");
        RackBlockEntity client = RackBlockEntity.potionShelf(rack.getBlockPos(), rack.getBlockState());
        client.handleUpdateTag(rack.getUpdateTag());
        helper.assertTrue(ItemStack.matches(client.getItem(11), potion) && material.equals(client.getMaterial()),
                "The client should receive both the displayed potion and the panel");

        BlockPos pos = rack.getBlockPos();
        var clone = rack.getBlockState().getBlock().getCloneItemStack(helper.getLevel(), pos, rack.getBlockState());
        helper.assertTrue(material.equals(FurnitureMaterial.fromItem(clone))
                        && !BlockItem.getBlockEntityData(clone).contains("Items"),
                "Cloning the rack should copy its panel");
        helper.getLevel().destroyBlock(pos, true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.LINGERING_POTION, RACK, 2, 1);
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                    .map(ItemEntity::getItem).toList();
            helper.assertTrue(drops.stream().anyMatch(drop -> ItemStack.matches(drop, potion)),
                    "The potion should drop with the same effect stored in the shelf");
            helper.assertTrue(drops.stream().anyMatch(drop -> drop.is(ModBlocks.POTION_SHELF.get().asItem())
                            && material.equals(FurnitureMaterial.fromItem(drop))
                            && !BlockItem.getBlockEntityData(drop).contains("Items")),
                    "The rack drop should contain the panel and its contents should drop separately");
        });
    }

    @GameTest(template = "empty")
    public static void potionAndToolRackRecipesPreserveMaterials(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        var level = helper.getLevel();
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            boolean framed = wood == FurnitureMaterial.Wood.FRAMED;
            var plank = framed ? ModItems.FRAMING_SHEET.get()
                    : BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(wood.id() + "_planks"));
            var slab = framed ? ModItems.FRAMING_BOARD.get()
                    : BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
            for (int slot = 0; slot < 9; slot++) {
                grid.setItem(slot, new ItemStack(slot == 3 || slot == 5 ? plank : slab));
            }
            grid.setItem(4, new ItemStack(Items.GLASS_BOTTLE));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack result = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(result.is(ModBlocks.POTION_SHELF.get().asItem())
                            && FurnitureMaterial.fromItem(result).wood() == wood,
                    "Matching " + wood.id() + " materials should craft the corresponding potion shelf");

            grid.setItem(3, new ItemStack(slab));
            grid.setItem(4, new ItemStack(Items.IRON_INGOT));
            grid.setItem(5, new ItemStack(slab));
            recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            result = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(result.is(ModBlocks.TOOL_RACK.get().asItem())
                            && FurnitureMaterial.fromItem(result).wood() == wood,
                    "Matching " + wood.id() + " materials should craft the corresponding tool rack");
        }
        helper.succeed();
    }

    private static void click(GameTestHelper helper, Player player, RackType type, int slot,
                              Direction opening, Direction side) {
        BlockPos pos = helper.absolutePos(RACK);
        double right = (slot % type.columns() + 0.5) / type.columns() - 0.5;
        double up = 0.5 - (slot / type.columns() + 0.5) / type.rows();
        Vec3 center = Vec3.atCenterOf(pos).add(0, up, 0).relative(opening.getCounterClockWise(), right);
        BlockHitResult hit = helper.getBlockState(RACK).getShape(helper.getLevel(), pos)
                .clip(center.relative(side, 2), center.relative(side, -2), pos);
        helper.assertTrue(hit != null && hit.getDirection() == side, "The " + type + " rack should be clickable from " + side);
        helper.useBlock(RACK, player, hit);
    }
}
