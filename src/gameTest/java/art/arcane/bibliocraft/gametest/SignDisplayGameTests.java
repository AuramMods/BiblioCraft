package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FancySignBlock;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.block.SignDisplayBlock;
import art.arcane.bibliocraft.blockentity.FancySignBlockEntity;
import art.arcane.bibliocraft.blockentity.FancySignLayout;
import art.arcane.bibliocraft.blockentity.LabelBlockEntity;
import art.arcane.bibliocraft.menu.FancySignMenu;
import art.arcane.bibliocraft.menu.LabelMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class SignDisplayGameTests {
    private static final BlockPos DISPLAY = new BlockPos(2, 1, 2);
    private static final FurnitureMaterial BRICK_FRAME =
            new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("bricks"));

    private SignDisplayGameTests() {}

    @GameTest(template = "empty")
    public static void labelMenuPreservesAllThreeStacksAndDropsThemSeparately(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        place(helper, player, BRICK_FRAME.applyTo(new ItemStack(ModBlocks.LABEL.get())));
        var label = (LabelBlockEntity) helper.getBlockEntity(DISPLAY);
        player.setPos(Vec3.atCenterOf(label.getBlockPos()));
        var menu = new LabelMenu(1, player.getInventory(), label);
        helper.assertTrue(menu.slots.size() == 39 && menu.stillValid(player), "The label exposes all three original menu slots");
        player.getInventory().setItem(0, new ItemStack(Items.APPLE, 27));
        player.getInventory().setItem(1, new ItemStack(Items.GOLD_INGOT, 4));
        player.getInventory().setItem(2, new ItemStack(Items.EMERALD, 2));
        for (int index = 30; index < 33; index++) {
            helper.assertTrue(!menu.quickMoveStack(player, index).isEmpty(), "Shift-click moves a full stack to the next slot");
        }
        var state = label.getBlockState();
        BlockPos pos = label.getBlockPos();
        var client = new LabelBlockEntity(pos, state);
        client.handleUpdateTag(label.getUpdateTag());
        var loaded = (LabelBlockEntity) BlockEntity.loadStatic(pos, state, label.saveWithFullMetadata());
        helper.assertTrue(loaded != null && loaded.getItem(0).getCount() == 27 && loaded.getItem(1).getCount() == 4
                        && client.getItem(2).getCount() == 2 && BRICK_FRAME.equals(client.getMaterial())
                        && BRICK_FRAME.equals(FurnitureMaterial.fromItem(state.getBlock().getCloneItemStack(helper.getLevel(), pos, state))),
                "Stored stacks and the framed panel survive saves, sync, and pick-block");
        helper.getLevel().destroyBlock(pos, true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.APPLE, DISPLAY, 2, 27);
            helper.assertItemEntityCountIs(Items.GOLD_INGOT, DISPLAY, 2, 4);
            helper.assertItemEntityCountIs(Items.EMERALD, DISPLAY, 2, 2);
            helper.assertItemEntityCountIs(ModBlocks.LABEL.get().asItem(), DISPLAY, 2, 1);
            helper.assertTrue(BRICK_FRAME.equals(FurnitureMaterial.fromItem(getDrop(helper, ModBlocks.LABEL.get().asItem()).getItem())),
                    "The dropped empty label keeps the frame panel");
        });
    }

    @GameTest(template = "empty")
    public static void fancySignDrillMovesTheBoardAndFeetInEveryFacing(GameTestHelper helper) {
        var block = (FancySignBlock) ModBlocks.FANCY_SIGN.get();
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(DISPLAY);
        Player player = helper.makeMockSurvivalPlayer();
        player.setYRot(90);
        for (int pitch : new int[]{-50, 0, 50}) {
            player.setXRot(pitch);
            BlockPlaceContext placement = new BlockPlaceContext(useContext(player, pos.below()));
            var placed = block.getStateForPlacement(placement);
            AttachFace expected = pitch < 0 ? AttachFace.CEILING : pitch > 0 ? AttachFace.FLOOR : AttachFace.WALL;
            helper.assertTrue(placed.getValue(FancySignBlock.FACE) == expected && placed.getValue(SignDisplayBlock.FACING) == Direction.NORTH,
                    "The selected legacy mount and the visible face must follow the player's pitch and yaw");
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (AttachFace mount : AttachFace.values()) {
                for (int shift = 0; shift < 3; shift++) {
                    var state = block.defaultBlockState().setValue(FancySignBlock.FACE, mount)
                            .setValue(SignDisplayBlock.FACING, facing).setValue(FancySignBlock.SHIFT, shift);
                    var shape = state.getShape(level, pos, CollisionContext.empty());
                    double radius = 0.47 - FancySignBlock.depth(state);
                    Vec3 center = new Vec3(0.5, 0.5, 0.5).relative(facing.getCounterClockWise(), radius);
                    helper.assertTrue(shape.toAabbs().stream().anyMatch(box -> box.contains(center))
                                    && state.getCollisionShape(level, pos).isEmpty(),
                            "Every mount retains a selectable board at the authored depth and leaves the cell walkable");
                    double minY = mount == AttachFace.FLOOR ? 0 : 0.2;
                    double maxY = mount == AttachFace.CEILING ? 1 : 0.8;
                    helper.assertTrue(shape.bounds().minY == minY && shape.bounds().maxY == maxY,
                            "Selecting a sign includes only the chosen mount's legs");
                }
            }
        }
        helper.setBlock(DISPLAY, block);
        ItemStack handDrill = new ItemStack(ModItems.HAND_DRILL.get());
        ItemStack poweredDrill = new ItemStack(ModItems.BIBLIO_DRILL.get());
        drill(player, handDrill, pos);
        helper.assertBlockProperty(DISPLAY, FancySignBlock.SHIFT, 1);
        drill(player, poweredDrill, pos);
        helper.assertBlockProperty(DISPLAY, FancySignBlock.SHIFT, 2);
        drill(player, handDrill, pos);
        helper.assertBlockProperty(DISPLAY, FancySignBlock.SHIFT, 0);
        player.setShiftKeyDown(true);
        drill(player, poweredDrill, pos);
        helper.assertBlockProperty(DISPLAY, SignDisplayBlock.FACING, Direction.EAST);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void signEditorsMergeChangesAndRespectCurrentAccess(GameTestHelper helper) {
        helper.setBlock(DISPLAY, ModBlocks.FANCY_SIGN.get());
        var sign = (FancySignBlockEntity) helper.getBlockEntity(DISPLAY);
        var owner = helper.makeMockSurvivalPlayer();
        var guest = helper.makeMockSurvivalPlayer();
        for (Player player : new Player[]{owner, guest}) {
            player.setPos(Vec3.atCenterOf(sign.getBlockPos()));
            player.containerMenu = new FancySignMenu(1, player.getInventory(), sign);
        }
        var first = (FancySignMenu) owner.containerMenu;
        var second = (FancySignMenu) guest.containerMenu;
        var edit = withLine(0, "§2Trades §l→", 3, new FancySignLayout.Icon(6, 2, -25, 15));
        helper.assertTrue(!first.save(guest, edit) && first.save(owner, edit),
                "Only the player who has this exact menu open can submit its edits");
        var later = withLine(1, "Glass & copper", 0, FancySignLayout.DEFAULT.left());
        helper.assertTrue(second.save(guest, later) && sign.getLayout().lines().get(0).equals(edit.lines().get(0))
                        && sign.getLayout().lines().get(1).equals(later.lines().get(1))
                        && sign.getLayout().left().equals(edit.left()),
                "Two simultaneous editors should preserve changes to different lines and icons");
        var client = new FancySignBlockEntity(sign.getBlockPos(), sign.getBlockState());
        client.handleUpdateTag(sign.getUpdateTag());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            sign.getLayout().write(buffer);
            helper.assertTrue(sign.getLayout().equals(client.getLayout()) && sign.getLayout().equals(FancySignLayout.read(buffer)),
                    "The update tag and wire format preserve Unicode, formatting, and icon geometry");
        } finally {
            buffer.release();
        }
        sign.toggleLock(owner);
        helper.assertTrue(!second.save(guest, edit), "A lock should invalidate an existing guest editor");
        owner.setPos(Vec3.atCenterOf(sign.getBlockPos()).add(12, 0, 0));
        helper.assertTrue(!first.save(owner, later), "Distance should invalidate the owner editor as well");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void signSurvivalHarvestPacksBothIconsAndTheLayoutOnce(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        place(helper, player, BRICK_FRAME.applyTo(new ItemStack(ModBlocks.FANCY_SIGN.get())));
        var sign = (FancySignBlockEntity) helper.getBlockEntity(DISPLAY);
        player.setPos(Vec3.atCenterOf(sign.getBlockPos()));
        var menu = new FancySignMenu(1, player.getInventory(), sign);
        var named = new ItemStack(Items.DIAMOND, 12);
        named.setHoverName(Component.literal("Market token"));
        player.getInventory().setItem(0, named);
        menu.quickMoveStack(player, 29);
        menu.quickMoveStack(player, 29);
        helper.assertTrue(sign.getItem(0).getCount() == 1 && sign.getItem(1).getCount() == 1
                        && player.getInventory().getItem(0).getCount() == 10,
                "Each icon slot accepts exactly one actual item through a menu transfer");
        var layout = withLine(0, "§6Market", 4, new FancySignLayout.Icon(5, 1, -45, 10));
        sign.setLayout(layout);
        var loaded = (FancySignBlockEntity) BlockEntity.loadStatic(sign.getBlockPos(), sign.getBlockState(), sign.saveWithFullMetadata());
        helper.assertTrue(loaded != null && loaded.getLayout().equals(layout) && loaded.getItem(0).hasCustomHoverName(),
                "Saving a placed sign preserves the formatted text and actual item data");
        var level = helper.getLevel();
        level.destroyBlock(sign.getBlockPos(), true);
        helper.assertItemEntityCountIs(ModBlocks.FANCY_SIGN.get().asItem(), DISPLAY, 2, 1);
        helper.assertItemEntityCountIs(Items.DIAMOND, DISPLAY, 2, 0);
        var droppedEntity = getDrop(helper, ModBlocks.FANCY_SIGN.get().asItem());
        ItemStack dropped = droppedEntity.getItem().copy();
        droppedEntity.discard();
        place(helper, player, dropped);
        var replaced = (FancySignBlockEntity) helper.getBlockEntity(DISPLAY);
        helper.assertTrue(BRICK_FRAME.equals(replaced.getMaterial()) && layout.equals(replaced.getLayout())
                        && replaced.getItem(0).getCount() == 1 && replaced.getItem(1).getCount() == 1
                        && replaced.getItem(0).getHoverName().getString().equals("Market token"),
                "Normal player placement should unpack the drop with the panel, layout, and both real icon items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bothSignRecipesCarryEveryWoodAndTheFramedPanel(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            boolean framed = wood == FurnitureMaterial.Wood.FRAMED;
            var slab = framed ? ModItems.FRAMING_BOARD.get()
                    : ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
            grid.clearContent();
            for (int slot = 0; slot < 6; slot++) {
                grid.setItem(slot, new ItemStack(slab));
            }
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack label = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(label.is(ModBlocks.LABEL.get().asItem()) && FurnitureMaterial.fromItem(label).wood() == wood,
                    "The six-slab label recipe keeps the selected material");
            if (framed) {
                BRICK_FRAME.applyTo(label);
            }
            grid.clearContent();
            grid.setItem(0, label);
            grid.setItem(1, new ItemStack(Items.CHERRY_SIGN));
            grid.setItem(2, new ItemStack(Items.PAPER));
            recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            ItemStack sign = recipe.assemble(grid, level.registryAccess());
            helper.assertTrue(sign.is(ModBlocks.FANCY_SIGN.get().asItem())
                            && FurnitureMaterial.fromItem(label).equals(FurnitureMaterial.fromItem(sign)),
                    "A vanilla sign and paper upgrade the label without losing its wood or custom panel");
        }
        helper.succeed();
    }

    private static FancySignLayout withLine(int index, String text, int size, FancySignLayout.Icon left) {
        var lines = new ArrayList<>(FancySignLayout.DEFAULT.lines());
        lines.set(index, new FancySignLayout.Line(text, size));
        return new FancySignLayout(lines, left, FancySignLayout.DEFAULT.right());
    }

    private static void place(GameTestHelper helper, Player player, ItemStack stack) {
        helper.setBlock(DISPLAY.below(), Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(useContext(player, helper.absolutePos(DISPLAY.below())));
    }

    private static UseOnContext useContext(Player player, BlockPos pos) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false));
    }

    private static void drill(Player player, ItemStack drill, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        drill.getItem().onItemUseFirst(drill, useContext(player, pos));
    }

    private static ItemEntity getDrop(GameTestHelper helper, Item item) {
        BlockPos pos = helper.absolutePos(DISPLAY);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5),
                entity -> entity.getItem().is(item)).get(0);
    }
}
