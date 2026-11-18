package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

public final class DiscRackBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int SLOT_COUNT = 9;
    private static final TagKey<Item> DISCS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "disc_rack_contents"));

    public DiscRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISC_RACK.get(), pos, state, SLOT_COUNT);
    }

    public static boolean isDisc(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof RecordItem || stack.is(DISCS) || stack.is(ModBlocks.DINNER_PLATE.get().asItem())) {
            return true;
        }
        String id = stack.getDescriptionId().toLowerCase(Locale.ROOT);
        if (id.contains("rack")) {
            return false;
        }
        if (id.contains("filmreel") || id.contains("film_reel") || id.contains("clapper") || id.contains("dinnerplate")) {
            return true;
        }
        for (String allowed : BiblioConfig.COMMON.additionalDiscs.get().split(",")) {
            String keyword = allowed.strip().toLowerCase(Locale.ROOT);
            if (!keyword.isEmpty() && id.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isDisc(stack);
    }
}
