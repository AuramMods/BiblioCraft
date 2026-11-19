package art.arcane.bibliocraft.recipe;

import art.arcane.bibliocraft.blockentity.ClipboardBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ClipboardBlockRecipe extends CustomRecipe {
    public ClipboardBlockRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return BiblioConfig.COMMON.enableClipboard.get() && !input(container).isEmpty();
    }

    private static ItemStack input(CraftingContainer container) {
        ItemStack found = ItemStack.EMPTY;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                if (!found.isEmpty() || !stack.is(ModItems.BIBLIO_CLIPBOARD.get())) {
                    return ItemStack.EMPTY;
                }
                found = stack;
            }
        }
        return found;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        ItemStack source = input(container);
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack output = new ItemStack(ModBlocks.CLIPBOARD.get());
        output.getOrCreateTagElement("BlockEntityTag").put(ClipboardBlockEntity.STACK_TAG,
                source.copyWithCount(1).save(new CompoundTag()));
        return output;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width > 0 && height > 0;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CLIPBOARD_BLOCK.get();
    }
}
