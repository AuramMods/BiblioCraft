package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.map.AtlasContents;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.geometry.StandaloneGeometryBakingContext;
import net.minecraftforge.client.model.obj.ObjLoader;
import net.minecraftforge.client.model.obj.ObjModel;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

record AtlasModel(ObjModel original) implements IUnbakedGeometry<AtlasModel> {
    static AtlasModel read(JsonObject json, JsonDeserializationContext context) {
        return new AtlasModel(ObjLoader.INSTANCE.read(json, context));
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites,
                           ModelState state, ItemOverrides overrides, ResourceLocation location) {
        BakedModel[] positions = new BakedModel[36];
        ItemOverrides needle = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                      @Nullable LivingEntity entity, int seed) {
                var target = AtlasContents.displayedCompass(stack);
                if (target == null) {
                    return model;
                }
                if (entity == null || target.dimension() != null && !target.dimension().equals(entity.level().dimension().location())) {
                    return positions[35];
                }
                int part = WaypointCompassItem.needlePart(entity.getX(), entity.getZ(), entity.getYRot(), target);
                return positions[part - 1];
            }
        };
        for (int index = 0; index < positions.length; index++) {
            var frame = visible(context, location, index + 1);
            positions[index] = original.bake(frame, baker, sprites, state, ItemOverrides.EMPTY, location);
        }
        return original.bake(visible(context, location, 0), baker, sprites, state, needle, location);
    }

    private static IGeometryBakingContext visible(IGeometryBakingContext context, ResourceLocation location, int needle) {
        var parts = new Object2BooleanOpenHashMap<String>();
        parts.put("book", true);
        parts.put("compass", needle != 0);
        for (int part = 1; part <= 36; part++) {
            parts.put(Integer.toString(part), part == needle);
        }
        return StandaloneGeometryBakingContext.builder(context).withVisibleComponents(parts).build(location);
    }
}
