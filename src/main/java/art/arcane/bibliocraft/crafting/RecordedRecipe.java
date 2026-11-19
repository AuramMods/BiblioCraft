package art.arcane.bibliocraft.crafting;

import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record RecordedRecipe(NonNullList<ItemStack> grid, ItemStack output, @Nullable ResourceLocation recipeId) {
    public static final int GRID_SIZE = 9;
    public static final int TEXT_LINES = 10;
    public static final int LINE_LENGTH = 24;

    public static RecordedRecipe read(ItemStack stack) {
        var grid = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        CompoundTag tag = stack.getTag();
        if (!stack.is(ModItems.RECIPE_BOOK.get()) || tag == null) {
            return new RecordedRecipe(grid, ItemStack.EMPTY, null);
        }
        ListTag stored = tag.getList("grid", Tag.TAG_COMPOUND);
        for (int index = 0; index < stored.size(); index++) {
            CompoundTag entry = stored.getCompound(index);
            int slot = entry.getByte("Slot");
            if (slot >= 0 && slot < grid.size()) {
                grid.set(slot, ItemStack.of(entry).copyWithCount(1));
            }
        }
        return new RecordedRecipe(grid, ItemStack.of(tag.getCompound("result")),
                tag.contains("RecipeId", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("RecipeId")) : null);
    }

    public static ItemStack create(List<ItemStack> items, ItemStack output, ResourceLocation id) {
        ItemStack book = new ItemStack(ModItems.RECIPE_BOOK.get());
        ListTag grid = new ListTag();
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                CompoundTag entry = stack.copyWithCount(1).save(new CompoundTag());
                entry.putByte("Slot", (byte) slot);
                grid.add(entry);
            }
        }
        CompoundTag tag = book.getOrCreateTag();
        tag.put("grid", grid);
        tag.put("result", output.save(new CompoundTag()));
        tag.putString("RecipeId", id.toString());
        return book;
    }

    public boolean isRecorded() {
        return !output.isEmpty() && grid.stream().anyMatch(stack -> !stack.isEmpty());
    }

    public static boolean isSigned(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean("signed");
    }

    public static List<String> description(ItemStack stack) {
        var tag = stack.getTag();
        ListTag text = tag == null ? new ListTag() : tag.getList("text", Tag.TAG_STRING);
        return java.util.stream.IntStream.range(0, TEXT_LINES)
                .mapToObj(index -> text.getString(index))
                .map(line -> art.arcane.bibliocraft.catalog.CatalogContents.bounded(line, LINE_LENGTH))
                .toList();
    }
}
