package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.block.PaintingFrameBlock;
import art.arcane.bibliocraft.block.PaintingPressBlock;
import art.arcane.bibliocraft.blockentity.PaintingFrameBlockEntity;
import art.arcane.bibliocraft.blockentity.PaintingPressBlockEntity;
import art.arcane.bibliocraft.menu.PaintingFrameMenu;
import art.arcane.bibliocraft.painting.PaintingArt;
import art.arcane.bibliocraft.painting.PaintingSettings;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class PaintingGameTests {
    private static final BlockPos PLACE = new BlockPos(2, 1, 2);
    private static final List<Block> FRAMES = List.of(ModBlocks.PAINTING_FRAME_BORDERLESS.get(),
            ModBlocks.PAINTING_FRAME_FLAT.get(), ModBlocks.PAINTING_FRAME_SIMPLE.get(),
            ModBlocks.PAINTING_FRAME_MIDDLE.get(), ModBlocks.PAINTING_FRAME_FANCY.get());

    private PaintingGameTests() {}

    @GameTest(template = "empty")
    public static void pressTransfersOneCanvasAtItsOriginalMidpoint(GameTestHelper helper) {
        helper.setBlock(PLACE, ModBlocks.PAINTING_PRESS.get());
        var press = (PaintingPressBlockEntity) helper.getBlockEntity(PLACE);
        var player = player(helper);
        var art = PaintingArt.validate(PaintingArt.Source.VANILLA, "Kebab", 1, 1).orElseThrow();
        press.select(art);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PAINTING_CANVAS.get(), 3));
        helper.useBlock(PLACE, player, handle(press.getBlockPos(), Direction.NORTH));
        helper.assertTrue(player.getMainHandItem().getCount() == 2 && press.getItem(0).getCount() == 1
                && PaintingArt.from(press.getItem(0)).isBlank(), "Insertion should keep the other two canvases");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(PLACE, player, handle(press.getBlockPos(), Direction.NORTH));
        helper.assertTrue(press.progress() > 0 && !press.startTransfer(), "The front handle should start one print cycle");
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            var state = press.getBlockState().setValue(PaintingPressBlock.FACING, dir);
            helper.assertTrue(PaintingPressBlock.isHandle(state, handle(press.getBlockPos(), dir)),
                    "The handle must rotate with the model");
        }
        var saved = press.saveWithFullMetadata();
        var restored = (PaintingPressBlockEntity) BlockEntity.loadStatic(press.getBlockPos(), press.getBlockState(), saved);
        restored.setLevel(helper.getLevel());
        press.select(PaintingArt.bundled().get(0));
        for (int tick = 1; tick < PaintingPressBlockEntity.TRANSFER_TICK - 1; tick++) {
            restored.tick(helper.getLevel());
        }
        helper.assertTrue(PaintingArt.from(restored.getItem(0)).isBlank(), "Artwork should transfer as the lid closes");
        restored.tick(helper.getLevel());
        helper.assertTrue(PaintingArt.from(restored.getItem(0)).equals(art) && restored.getItem(0).getMaxStackSize() == 1,
                "The saved job should restore the original selected art and make the canvas non-stackable");
        helper.succeedWhen(() -> helper.assertTrue(press.progress() == 0
                && PaintingArt.from(press.getItem(0)).equals(art), "The live ticker should finish the same transfer"));
    }

    @GameTest(template = "empty")
    public static void frameLinksChooseOriginalMiterAndConcaveProfiles(GameTestHelper helper) {
        var base = frame(helper, PLACE, ModBlocks.PAINTING_FRAME_FANCY.get());
        var above = frame(helper, PLACE.above(), ModBlocks.PAINTING_FRAME_FANCY.get());
        var right = frame(helper, PLACE.north(), ModBlocks.PAINTING_FRAME_FLAT.get());
        var diagonal = frame(helper, PLACE.north().above(), ModBlocks.PAINTING_FRAME_SIMPLE.get());
        var player = player(helper);
        int singles = 3 | 1 << 3 | 1 << 7 | 1 << 11 | 1 << 15;
        helper.assertTrue(parts(helper, base) == singles, "Unjoined frames should show four double miters");
        helper.assertTrue(base.toggleLink(0, player) && base.toggleLink(1, player), "Adjacent frames can join across styles");
        helper.assertTrue((parts(helper, base) & 1 << 19) != 0 && (parts(helper, base) & 4) != 0,
                "An L-shaped join needs the original inside corner and full canvas");
        helper.assertTrue(above.toggleLink(1, player) && right.toggleLink(0, player) && diagonal.activeLinks(helper.getLevel()) == 12,
                "All four links must be reciprocal");
        helper.assertTrue((parts(helper, base) & 1 << 19) == 0, "A closed square should have no inside corner");
        helper.setBlock(PLACE.north(), Blocks.AIR);
        helper.assertTrue(base.links() == 1 && diagonal.links() == 8 && (parts(helper, base) & 1 << 19) == 0,
                "Breaking a frame must clear both affected joins");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void frameEditsMergeAndDropsRetainMaterialAndArt(GameTestHelper helper) {
        var frame = frame(helper, PLACE, ModBlocks.PAINTING_FRAME_BORDERLESS.get());
        var neighbor = frame(helper, PLACE.above(), ModBlocks.PAINTING_FRAME_FANCY.get());
        var art = PaintingArt.validate(PaintingArt.Source.CUSTOM, "gallery/mural.png", 2, 3).orElseThrow();
        var blockMaterial = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("bricks"));
        var tag = frame.saveWithFullMetadata();
        blockMaterial.save(tag);
        frame.load(tag);
        var first = player(helper);
        var second = player(helper);
        frame.connectBorderless(first);
        helper.assertTrue(frame.activeLinks(helper.getLevel()) == 1 && neighbor.activeLinks(helper.getLevel()) == 4,
                "A placed borderless frame should join with its co-planar neighbor");
        var stack = new ItemStack(ModItems.PAINTING_CANVAS.get());
        art.save(stack.getOrCreateTag());
        first.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        helper.useBlock(PLACE, first, new BlockHitResult(Vec3.atCenterOf(frame.getBlockPos()), Direction.EAST,
                frame.getBlockPos(), false));
        var original = frame.settings();
        helper.assertTrue(original.customWidth() == 2 && original.customHeight() == 3,
                "A placed custom canvas should start with the uploaded aspect ratio");
        var one = new PaintingFrameMenu(1, first.getInventory(), frame);
        var two = new PaintingFrameMenu(2, second.getInventory(), frame);
        first.containerMenu = one;
        second.containerMenu = two;
        helper.assertTrue(one.edit(first, original, new PaintingSettings(0, 3, 0, 0, 0, 2, 3, false))
                && two.edit(second, original, new PaintingSettings(2, 1, 0, 0, 1, 2, 3, true)),
                "Two people should be able to edit separate layout fields");
        var layout = frame.settings();
        helper.assertTrue(layout.scale() == 3 && layout.rotation() == 1 && layout.corner() == 2
                        && parts(helper, frame) == 0
                        && frame.getBlockState().getCollisionShape(helper.getLevel(), frame.getBlockPos(),
                                CollisionContext.empty()).isEmpty(),
                "The hidden frame should save merged settings and permit passage");
        var bounds = layout.footprint(art, 0);
        helper.assertTrue(bounds.left() == -8 && bounds.right() == 1 && bounds.bottom() == -5 && bounds.top() == 1
                && frame.getRenderBoundingBox().getZsize() > 9, "The rotated mural bounds must follow the master corner");
        var restored = (PaintingFrameBlockEntity) BlockEntity.loadStatic(frame.getBlockPos(),
                frame.getBlockState(), frame.saveWithFullMetadata());
        helper.assertTrue(restored.settings().equals(layout) && restored.art().equals(art),
                "The canvas and hidden mural settings must survive save/load");
        frame.toggleLock(first);
        helper.assertTrue(!two.edit(second, original, PaintingSettings.DEFAULT) && !neighbor.toggleLink(2, second),
                "A locked mural must reject editing and relinking by a guest");
        frame.toggleLock(first);
        helper.getLevel().destroyBlock(frame.getBlockPos(), true);
        var items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(frame.getBlockPos()).inflate(1));
        helper.assertTrue(items.stream().filter(drop -> ItemStack.matches(drop.getItem(), stack)).count() == 1
                        && items.stream().anyMatch(drop -> drop.getItem().is(ModBlocks.PAINTING_FRAME_BORDERLESS.get().asItem())
                        && FurnitureMaterial.fromItem(drop.getItem()).equals(blockMaterial)),
                "A broken frame and its one custom canvas must drop separately with their tags");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void galleryDataRoundTripsAndRestoresAllCanvasSizes(GameTestHelper helper) {
        List<PaintingArt> gallery = PaintingArt.bundled();
        helper.assertTrue(gallery.size() == 10, "All ten original BiblioCraft galleries should be available");
        for (PaintingArt art : gallery) {
            for (int aspect = 0; aspect < art.aspectCount(); aspect++) {
                for (int res = 0; res < art.image(aspect).textures().size(); res++) {
                    var texture = art.texture(aspect, res);
                    helper.assertTrue(PaintingGameTests.class.getResource("/assets/" + texture.getNamespace() + "/" + texture.getPath()) != null,
                            "Every original aspect and resolution must have its artwork");
                }
            }
        }
        var invalid = PaintingArt.validate(PaintingArt.Source.CUSTOM, "../not-art.png", 2, 3);
        helper.assertTrue(invalid.isEmpty(), "Custom lookup must stay in the artwork directory");
        var custom = PaintingArt.validate(PaintingArt.Source.CUSTOM, "wall/example.png", 4, 3).orElseThrow();
        var layout = new PaintingSettings(2, 4, 1, 0, 3, 4, 3, false);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            custom.write(buffer);
            layout.write(buffer);
            helper.assertTrue(PaintingArt.read(buffer).equals(custom) && PaintingSettings.read(buffer).equals(layout)
                    && !buffer.isReadable(), "Artwork and layout should round-trip through their menu codecs");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void eachFrameStyleCraftsInEveryFurnitureMaterial(GameTestHelper helper) {
        var player = player(helper);
        var grid = new TransientCraftingContainer(new CraftingMenu(1, player.getInventory()), 3, 3);
        var registries = helper.getLevel().registryAccess();
        String[][] patterns = {{"ISI", "SSS", "ISI"}, {"SSS", "SFS", "SSS"}, {"ISI", "SFS", "ISI"},
                {"ISI", "IFI", "ISI"}, {"III", "IFI", "III"}};
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            Item slab = wood == FurnitureMaterial.Wood.FRAMED ? ModItems.FRAMING_BOARD.get()
                    : BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(wood.id() + "_slab"));
            var material = new FurnitureMaterial(wood,
                    wood == FurnitureMaterial.Wood.FRAMED ? ResourceLocation.withDefaultNamespace("bricks") : null);
            for (int style = 0; style < FRAMES.size(); style++) {
                for (int slot = 0; slot < 9; slot++) {
                    grid.setItem(slot, switch (patterns[style][slot / 3].charAt(slot % 3)) {
                        case 'S' -> new ItemStack(slab);
                        case 'F' -> material.applyTo(new ItemStack(FRAMES.get(0)));
                        default -> new ItemStack(Items.STICK);
                    });
                }
                String path = BuiltInRegistries.BLOCK.getKey(FRAMES.get(style)).getPath();
                String recipe = wood == FurnitureMaterial.Wood.OAK ? path : path + "_" + wood.id();
                var match = helper.getLevel().getRecipeManager()
                        .byKey(ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, recipe)).orElseThrow();
                var shaped = (net.minecraft.world.item.crafting.CraftingRecipe) match;
                var result = shaped.assemble(grid, registries);
                helper.assertTrue(shaped.matches(grid, helper.getLevel()) && result.is(FRAMES.get(style).asItem())
                                && material.wood() == FurnitureMaterial.fromItem(result).wood()
                                && (style == 0 || material.equals(FurnitureMaterial.fromItem(result))),
                        "Each legacy pattern should craft its own style and preserve the chosen wood and custom panel");
            }
        }
        helper.succeed();
    }

    private static BlockHitResult handle(BlockPos pos, Direction facing) {
        Direction front = facing.getClockWise();
        return new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0, .9, 0).relative(front, .4), front, pos, false);
    }

    private static PaintingFrameBlockEntity frame(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, block);
        return (PaintingFrameBlockEntity) helper.getBlockEntity(pos);
    }

    private static int parts(GameTestHelper helper, PaintingFrameBlockEntity frame) {
        return PaintingFrameBlock.visibleParts(helper.getLevel(), frame.getBlockPos(), frame.getBlockState());
    }

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(PLACE)).add(1, 0, 0));
        return player;
    }
}
