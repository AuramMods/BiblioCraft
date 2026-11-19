package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.item.DeathCompassItem;
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

record WaypointCompassModel(ObjModel original) implements IUnbakedGeometry<WaypointCompassModel> {
    static WaypointCompassModel read(JsonObject json, JsonDeserializationContext context) {
        return new WaypointCompassModel(ObjLoader.INSTANCE.read(json, context));
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                           Function<Material, TextureAtlasSprite> sprites, ModelState state,
                           ItemOverrides overrides, ResourceLocation location) {
        BakedModel[] positions = new BakedModel[36];
        ItemOverrides needleOverride = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                      @Nullable LivingEntity entity, int seed) {
                if (entity == null) {
                    return model;
                }
                var target = stack.getItem() instanceof DeathCompassItem ? DeathCompassItem.target(stack)
                        : WaypointCompassItem.getTarget(stack);
                if (target == null || target.dimension() != null && !target.dimension().equals(entity.level().dimension().location())) {
                    return model;
                }
                int part = WaypointCompassItem.needlePart(entity.getX(), entity.getZ(), entity.getYRot(), target);
                return positions[part - 1];
            }
        };
        for (int index = 0; index < positions.length; index++) {
            var visible = new Object2BooleanOpenHashMap<String>();
            visible.put("face", true);
            for (int part = 1; part <= positions.length; part++) {
                visible.put(Integer.toString(part), part == index + 1);
            }
            var frame = StandaloneGeometryBakingContext.builder(context).withVisibleComponents(visible).build(location);
            positions[index] = original.bake(frame, baker, sprites, state, needleOverride, location);
        }
        return positions[35];
    }
}
