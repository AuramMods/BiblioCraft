package art.arcane.bibliocraft.recipe;

import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.item.ColoredBlockItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class ColoredBlockRecolorRecipe extends CustomRecipe {
    private final ColoredBlockItem item;
    private final Serializer serializer;

    private ColoredBlockRecolorRecipe(ResourceLocation id, CraftingBookCategory category,
                                      ColoredBlockItem item, Serializer serializer) {
        super(id, category);
        this.item = item;
        this.serializer = serializer;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return BiblioFeatureToggles.isBlockItemEnabled(ForgeRegistries.ITEMS.getKey(item))
                && !assemble(container, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        ItemStack furniture = ItemStack.EMPTY;
        DyeColor color = null;
        for (ItemStack stack : container.getItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (furniture.isEmpty() && stack.is(item)) {
                furniture = stack;
            } else if (color == null && stack.getItem() instanceof DyeItem dye) {
                color = dye.getDyeColor();
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (furniture.isEmpty() || color == null || color == ColoredBlockItem.getColor(furniture)) {
            return ItemStack.EMPTY;
        }
        return ColoredBlockItem.applyColor(furniture.copyWithCount(1), color);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    public static final class Serializer implements RecipeSerializer<ColoredBlockRecolorRecipe> {
        @Nullable
        private final Supplier<Item> defaultItem;

        public Serializer() {
            this(null);
        }

        public Serializer(@Nullable Supplier<Item> defaultItem) {
            this.defaultItem = defaultItem;
        }

        @Override
        public ColoredBlockRecolorRecipe fromJson(ResourceLocation id, JsonObject json) {
            Item item = defaultItem != null ? defaultItem.get()
                    : CraftingHelper.getItem(GsonHelper.getAsString(json, "item"), true);
            if (!(item instanceof ColoredBlockItem colored)) {
                throw new JsonSyntaxException("Expected a colorable furniture item in " + id);
            }
            return new ColoredBlockRecolorRecipe(id, CraftingBookCategory.CODEC.byName(
                    GsonHelper.getAsString(json, "category", "misc"), CraftingBookCategory.MISC), colored, this);
        }

        @Override
        public ColoredBlockRecolorRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            var category = buffer.readEnum(CraftingBookCategory.class);
            return new ColoredBlockRecolorRecipe(id, category, (ColoredBlockItem) buffer.readById(BuiltInRegistries.ITEM), this);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ColoredBlockRecolorRecipe recipe) {
            buffer.writeEnum(recipe.category());
            buffer.writeId(BuiltInRegistries.ITEM, recipe.item);
        }
    }
}
