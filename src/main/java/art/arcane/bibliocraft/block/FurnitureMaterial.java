package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.BiblioCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Locale;

public record FurnitureMaterial(Wood wood, @Nullable ResourceLocation panel) {
    public static final FurnitureMaterial OAK = new FurnitureMaterial(Wood.OAK, null);
    public static final String TAG = "FurnitureMaterial";
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    public FurnitureMaterial {
        if (wood != Wood.FRAMED) {
            panel = null;
        }
    }

    public static FurnitureMaterial fromItem(ItemStack stack) {
        return fromTag(stack.getTagElement(BLOCK_ENTITY_TAG));
    }

    public static FurnitureMaterial fromTag(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains(TAG)) {
            return OAK;
        }
        CompoundTag saved = tag.getCompound(TAG);
        Wood wood = Wood.byName(saved.getString("wood"));
        ResourceLocation panel = saved.getString("panel").isEmpty()
                ? null : ResourceLocation.tryParse(saved.getString("panel"));
        return new FurnitureMaterial(wood, panel);
    }

    public ItemStack applyTo(ItemStack stack) {
        CompoundTag blockEntity = stack.getTagElement(BLOCK_ENTITY_TAG);
        if (!equals(OAK)) {
            save(stack.getOrCreateTagElement(BLOCK_ENTITY_TAG));
        } else if (blockEntity != null) {
            save(blockEntity);
            if (blockEntity.isEmpty()) {
                stack.removeTagKey(BLOCK_ENTITY_TAG);
            }
        }
        return stack;
    }

    public void save(CompoundTag tag) {
        if (equals(OAK)) {
            tag.remove(TAG);
            return;
        }
        CompoundTag saved = new CompoundTag();
        saved.putString("wood", wood.id);
        if (panel != null) {
            saved.putString("panel", panel.toString());
        }
        tag.put(TAG, saved);
    }

    public enum Wood {
        OAK, SPRUCE, BIRCH, JUNGLE, ACACIA, DARK_OAK, MANGROVE, CHERRY, BAMBOO, CRIMSON, WARPED, FRAMED;

        private static final Wood[] VALUES = values();
        private final String id = name().toLowerCase(Locale.ROOT);

        public String id() {
            return id;
        }

        public ResourceLocation texture() {
            return this == FRAMED
                    ? ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "block/frame")
                    : ResourceLocation.withDefaultNamespace("block/" + id + "_planks");
        }

        private static Wood byName(String name) {
            for (Wood wood : VALUES) {
                if (wood.id.equals(name)) {
                    return wood;
                }
            }
            return OAK;
        }
    }
}
