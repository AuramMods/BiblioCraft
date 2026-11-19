package art.arcane.bibliocraft.recipe;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

import java.util.ArrayList;
import java.util.List;

public record RecipeFeatureCondition(List<ResourceLocation> items, boolean lockRecipe) implements ICondition {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "features_enabled");
    public static final IConditionSerializer<RecipeFeatureCondition> SERIALIZER = new Serializer();

    public RecipeFeatureCondition {
        items = List.copyOf(items);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return (!lockRecipe || BiblioConfig.COMMON.enableLockRecipe.get())
                && items.stream().allMatch(id -> BiblioFeatureToggles.isBlockItemEnabled(id)
                        && BiblioFeatureToggles.isItemEnabled(id));
    }

    private static final class Serializer implements IConditionSerializer<RecipeFeatureCondition> {
        @Override
        public void write(JsonObject json, RecipeFeatureCondition value) {
            var items = new JsonArray();
            value.items().forEach(id -> items.add(id.toString()));
            json.add("items", items);
            if (value.lockRecipe()) {
                json.addProperty("lock_recipe", true);
            }
        }

        @Override
        public RecipeFeatureCondition read(JsonObject json) {
            var items = new ArrayList<ResourceLocation>();
            for (var entry : GsonHelper.getAsJsonArray(json, "items")) {
                items.add(new ResourceLocation(GsonHelper.convertToString(entry, "item ID")));
            }
            return new RecipeFeatureCondition(items, GsonHelper.getAsBoolean(json, "lock_recipe", false));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
