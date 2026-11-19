package art.arcane.bibliocraft.gametest;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.ColorFacingEntityBlock;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.item.ColoredBlockItem;
import art.arcane.bibliocraft.item.FurnitureBlockItem;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BiblioCraft.MODID)
@PrefixGameTestTemplate(false)
public final class BlockLootGameTests {
    private BlockLootGameTests() {}

    @GameTest(template = "empty")
    public static void allPlacedBlocksKeepTheirDropIdsAndApplicableAppearance(GameTestHelper helper) {
        var relative = new BlockPos(2, 1, 2);
        var pos = helper.absolutePos(relative);
        var level = helper.getLevel();
        var framed = new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, ResourceLocation.withDefaultNamespace("bricks"));
        for (var entry : ModBlocks.BLOCKS.getEntries()) {
            var block = entry.get();
            var item = block.asItem();
            var state = block.defaultBlockState();
            if (item instanceof ColoredBlockItem) {
                state = state.setValue(ColorFacingEntityBlock.COLOR, DyeColor.MAGENTA);
            }
            helper.setBlock(relative, state);
            var furniture = level.getBlockEntity(pos);
            if (item instanceof FurnitureBlockItem && furniture instanceof FurnitureBlockEntity) {
                var data = furniture.saveWithFullMetadata();
                framed.save(data);
                furniture.load(data);
            }
            var drops = Block.getDrops(level.getBlockState(pos), level, pos, furniture);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(item) && drops.get(0).getCount() == 1,
                    entry.getId() + " should return one block with the same registry ID");
            var drop = drops.get(0);
            if (item instanceof FurnitureBlockItem) {
                helper.assertTrue(FurnitureMaterial.fromItem(drop).equals(framed),
                        entry.getId() + " should keep its custom wood panel");
            } else if (item instanceof ColoredBlockItem) {
                helper.assertTrue(ColoredBlockItem.getColor(drop) == DyeColor.MAGENTA,
                        entry.getId() + " should keep its dye");
            }
        }
        helper.succeed();
    }
}
