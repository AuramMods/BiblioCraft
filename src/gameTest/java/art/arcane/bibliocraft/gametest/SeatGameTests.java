package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.block.SeatBlock;
import art.arcane.bibliocraft.blockentity.FurniturePanelerBlockEntity;
import art.arcane.bibliocraft.blockentity.SeatBlockEntity;
import art.arcane.bibliocraft.entity.SeatEntity;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModEntities;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class SeatGameTests {
    private static final BlockPos SEAT = new BlockPos(2, 1, 2);

    private SeatGameTests() {}

    @GameTest(template = "empty")
    public static void seatAttachmentsPreserveBackMaterialAndBothColors(GameTestHelper helper) {
        SeatBlockEntity seat = placeSeat(helper);
        var frame = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("bricks"));
        var backWood = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("stone_bricks"));
        var base = new CompoundTag();
        frame.save(base);
        seat.load(base);
        var player = helper.makeMockSurvivalPlayer();
        var back = backWood.applyTo(new ItemStack(ModItems.SEAT_BACK_5.get(), 2));
        click(helper, player, SEAT, Direction.UP, new ItemStack(Items.PURPLE_CARPET, 4));
        helper.assertTrue(player.getMainHandItem().getCount() == 3, "A cushion should consume exactly one carpet");
        click(helper, player, SEAT, Direction.EAST, new ItemStack(Items.YELLOW_CARPET, 4));
        click(helper, player, SEAT, Direction.UP, back);
        helper.assertTrue(back.getCount() == 1 && seat.appearance().back() == 5
                        && backWood.equals(seat.appearance().backMaterial())
                        && seat.appearance().cushion().getName().equals("purple") && seat.appearance().carpet().getName().equals("yellow"),
                "All three attachments should stay independent and select the installed back's wood and shape");

        var actual = (SeatBlockEntity) BlockEntity.loadStatic(seat.getBlockPos(), seat.getBlockState(), seat.saveWithFullMetadata());
        var client = new SeatBlockEntity(seat.getBlockPos(), seat.getBlockState());
        client.handleUpdateTag(seat.getUpdateTag());
        helper.assertTrue(actual != null && seat.appearance().equals(actual.appearance()) && seat.appearance().equals(client.appearance()),
                "The complete seat appearance should survive saves and block entity updates");

        var drill = new ItemStack(ModItems.HAND_DRILL.get());
        drill(helper, player, SEAT, Direction.UP, drill);
        drill(helper, player, SEAT, Direction.EAST, drill);
        drill(helper, player, SEAT, Direction.EAST, drill);
        helper.succeedWhen(() -> {
            helper.assertTrue(seat.isEmpty(), "The drill should return all attachments in order");
            helper.assertItemEntityCountIs(Items.PURPLE_CARPET, SEAT, 2, 1);
            helper.assertItemEntityCountIs(Items.YELLOW_CARPET, SEAT, 2, 1);
            helper.assertItemEntityCountIs(ModItems.SEAT_BACK_5.get(), SEAT, 2, 1);
            helper.assertTrue(helper.getEntities(EntityType.ITEM, SEAT, 2).stream()
                            .filter(item -> item.getItem().is(ModItems.SEAT_BACK_5.get()))
                            .allMatch(item -> backWood.equals(FurnitureMaterial.fromItem(item.getItem()))),
                    "The removed back should keep its original panel");
        });
    }

    @GameTest(template = "empty")
    public static void seatsJoinAsBenchesAndClearLinksWhenBroken(GameTestHelper helper) {
        var seat = placeSeat(helper);
        var block = ModBlocks.SEAT.get();
        BlockPos east = SEAT.east();
        BlockPos north = SEAT.north();
        helper.setBlock(east, block.defaultBlockState());
        helper.setBlock(north, block.defaultBlockState().setValue(SeatBlock.FACING, Direction.WEST));
        var player = helper.makeMockSurvivalPlayer();
        player.setShiftKeyDown(true);
        var drill = new ItemStack(ModItems.BIBLIO_DRILL.get());
        drill(helper, player, SEAT, Direction.UP, drill);
        drill(helper, player, east, Direction.UP, drill);
        drill(helper, player, SEAT, Direction.UP, drill);
        drill(helper, player, north, Direction.UP, drill);
        helper.assertBlockProperty(SEAT, BlockStateProperties.EAST, true);
        helper.assertBlockProperty(SEAT, BlockStateProperties.NORTH, true);
        helper.assertBlockProperty(east, BlockStateProperties.WEST, true);
        helper.assertBlockProperty(north, BlockStateProperties.SOUTH, true);
        var level = helper.getLevel();
        BlockPos pos = seat.getBlockPos();
        var state = level.getBlockState(pos);
        int parts = SeatBlock.visibleParts(state, 4, false);
        helper.assertTrue(parts == bits(0, 1, 3, 4, 5, 7, 8, 10, 13, 14, 17, 24, 25, 29),
                "The bend should render north and east extensions, only unobstructed legs, and the correct connected back cloth");
        var shape = state.getCollisionShape(level, pos, CollisionContext.empty());
        helper.assertTrue(shape.toAabbs().stream().anyMatch(box -> box.contains(0.95, 0.65, 0.5))
                        && shape.toAabbs().stream().noneMatch(box -> box.contains(0.78, 0.2, 0.22)),
                "Bench collision should extend along the seam and leave the removed legs clear");
        drill(helper, player, SEAT, Direction.SOUTH, drill);
        helper.assertBlockProperty(SEAT, SeatBlock.FACING, Direction.EAST);
        helper.assertBlockProperty(SEAT, BlockStateProperties.EAST, true);
        level.destroyBlock(helper.absolutePos(east), true);
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(SEAT, BlockStateProperties.EAST, false);
            helper.assertBlockProperty(SEAT, BlockStateProperties.NORTH, true);
        });
    }

    @GameTest(template = "empty")
    public static void occupiedSeatPowersThroughStoneAndClearsOnDismount(GameTestHelper helper) {
        var seat = placeSeat(helper);
        var level = helper.getLevel();
        var pos = seat.getBlockPos();
        helper.setBlock(SEAT.east(), Blocks.STONE);
        helper.setBlock(SEAT.east(2), Blocks.REDSTONE_LAMP);
        var player = helper.makeMockSurvivalPlayer();
        var second = helper.makeMockSurvivalPlayer();
        click(helper, player, SEAT, Direction.UP, ItemStack.EMPTY);
        helper.assertTrue(player.getVehicle() instanceof SeatEntity mount && pos.equals(mount.seatPos())
                        && !SeatEntity.sit(level, pos, second), "An ordinary click should mount one player at a time");
        helper.assertBlockProperty(SEAT, SeatBlock.OCCUPIED, true);
        helper.assertBlockProperty(SEAT.east(2), RedstoneLampBlock.LIT, true);
        var mount = (SeatEntity) player.getVehicle();
        player.stopRiding();
        helper.assertBlockProperty(SEAT, SeatBlock.OCCUPIED, false);
        helper.startSequence().thenIdle(25).thenExecute(() -> {
            helper.assertBlockProperty(SEAT.east(2), RedstoneLampBlock.LIT, false);
            helper.assertTrue(mount.isRemoved(), "Dismounting should clear the output and clean up the empty anchor");
        }).thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void namedMobSitsAndCanBeLetOutOrDismountedByBreakingTheSeat(GameTestHelper helper) {
        var seat = placeSeat(helper);
        var pig = helper.spawn(EntityType.PIG, SEAT.south());
        pig.setNoAi(true);
        pig.setCustomName(Component.literal("Dumpling"));
        var level = helper.getLevel();
        var pos = seat.getBlockPos();
        var player = helper.makeMockSurvivalPlayer();
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(pig.getVehicle() instanceof SeatEntity,
                "A nearby named mob should find the empty seat on its next scan"))
                .thenExecute(() -> {
                    helper.assertBlockProperty(SEAT, SeatBlock.OCCUPIED, true);
                    player.setShiftKeyDown(true);
                    click(helper, player, SEAT, Direction.UP, ItemStack.EMPTY);
                    helper.assertTrue(!pig.isPassenger(), "A sneak click should release the named rider");
                    helper.assertBlockProperty(SEAT, SeatBlock.OCCUPIED, false);
                    helper.assertTrue(SeatEntity.sit(level, pos, player), "The released seat should be available immediately");
                    level.destroyBlock(pos, true);
                    helper.assertTrue(!player.isPassenger() && SeatEntity.find(level, pos) == null,
                            "Breaking the seat should eject its rider and remove its invisible anchor");
                    var saved = ModEntities.SEAT_ENTITY.get().create(level);
                    saved.setSeatPos(BlockPos.ZERO);
                    var restored = ModEntities.SEAT_ENTITY.get().create(level);
                    restored.load(saved.saveWithoutId(new CompoundTag()));
                    helper.assertTrue(BlockPos.ZERO.equals(restored.seatPos()), "The anchor at the world origin should survive save and load");
                }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void seatRecipesAndPanelerPreserveEveryMaterial(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockSurvivalPlayer();
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        List<Item> items = List.of(ModBlocks.SEAT.get().asItem(), ModItems.SEAT_BACK_1.get(), ModItems.SEAT_BACK_2.get(),
                ModItems.SEAT_BACK_3.get(), ModItems.SEAT_BACK_4.get(), ModItems.SEAT_BACK_5.get());
        String[][] shapes = {{" W ", " S ", "IXI"}, {" W ", " S ", "I I"}, {"IWI", "ISI", "I I"},
                {"BS "}, {"W  ", "S  "}, {"BSS"}};
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            var slab = wood == FurnitureMaterial.Wood.FRAMED ? ModItems.FRAMING_BOARD.get()
                    : ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
            for (int style = 0; style < items.size(); style++) {
                grid.clearContent();
                String[] rows = shapes[style];
                for (int row = 0; row < rows.length; row++) {
                    for (int column = 0; column < 3; column++) {
                        var ingredient = switch (rows[row].charAt(column)) {
                            case 'W' -> Items.WHITE_WOOL;
                            case 'S' -> slab;
                            case 'I' -> Items.STICK;
                            case 'X' -> Items.OAK_PRESSURE_PLATE;
                            case 'B' -> ModItems.SEAT_BACK_2.get();
                            default -> Items.AIR;
                        };
                        grid.setItem(row * 3 + column, new ItemStack(ingredient));
                    }
                }
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
                ItemStack result = recipe.assemble(grid, level.registryAccess());
                helper.assertTrue(result.is(items.get(style)) && FurnitureMaterial.fromItem(result).wood() == wood,
                        "All six legacy recipe patterns should preserve the selected slab material");
            }
        }
        helper.setBlock(SEAT, ModBlocks.FURNITURE_PANELER.get());
        var paneler = (FurniturePanelerBlockEntity) helper.getBlockEntity(SEAT);
        paneler.setItem(FurniturePanelerBlockEntity.PANELS, new ItemStack(Items.BRICKS));
        var frame = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, null);
        paneler.setItem(FurniturePanelerBlockEntity.FURNITURE, frame.applyTo(new ItemStack(ModItems.SEAT_BACK_3.get())));
        ItemStack result = paneler.getResult();
        helper.assertTrue(result.is(ModItems.SEAT_BACK_3.get())
                        && ResourceLocation.withDefaultNamespace("bricks").equals(FurnitureMaterial.fromItem(result).panel()),
                "Framed back items should accept a panel through the existing furniture paneler");
        paneler.clearContent();
        var seat = placeSeat(helper);
        var material = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("stone_bricks"));
        var saved = new CompoundTag();
        material.save(saved);
        seat.load(saved);
        seat.setItem(SeatBlockEntity.CUSHION, new ItemStack(Items.ORANGE_CARPET));
        seat.setItem(SeatBlockEntity.BACK, result);
        seat.setItem(SeatBlockEntity.CARPET, new ItemStack(Items.BLUE_CARPET));
        var pos = seat.getBlockPos();
        var state = seat.getBlockState();
        helper.assertTrue(material.equals(FurnitureMaterial.fromItem(state.getBlock().getCloneItemStack(level, pos, state))),
                "Pick-block should carry the seat panel without moving its attachments");
        level.destroyBlock(pos, true);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(ModBlocks.SEAT.get().asItem(), SEAT, 2, 1);
            helper.assertItemEntityCountIs(ModItems.SEAT_BACK_3.get(), SEAT, 2, 1);
            helper.assertItemEntityCountIs(Items.ORANGE_CARPET, SEAT, 2, 1);
            helper.assertItemEntityCountIs(Items.BLUE_CARPET, SEAT, 2, 1);
            helper.assertTrue(helper.getEntities(EntityType.ITEM, SEAT, 2).stream()
                            .anyMatch(item -> item.getItem().is(ModBlocks.SEAT.get().asItem())
                                    && material.equals(FurnitureMaterial.fromItem(item.getItem()))),
                    "The seat's own drop should retain its material while the attachments drop separately");
        });
    }

    private static SeatBlockEntity placeSeat(GameTestHelper helper) {
        helper.setBlock(SEAT.below(), Blocks.STONE);
        helper.setBlock(SEAT, ModBlocks.SEAT.get());
        return (SeatBlockEntity) helper.getBlockEntity(SEAT);
    }

    private static void click(GameTestHelper helper, Player player, BlockPos relative, Direction side, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        helper.useBlock(relative, player, hit(helper.absolutePos(relative), side));
    }

    private static void drill(GameTestHelper helper, Player player, BlockPos relative, Direction side, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        held.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper.absolutePos(relative), side)));
    }

    private static BlockHitResult hit(BlockPos pos, Direction face) {
        return new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5), face, pos, false);
    }

    private static int bits(int... indexes) {
        int mask = 0;
        for (int index : indexes) {
            mask |= 1 << index;
        }
        return mask;
    }
}
