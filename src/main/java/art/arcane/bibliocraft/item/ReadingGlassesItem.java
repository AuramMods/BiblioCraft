package art.arcane.bibliocraft.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class ReadingGlassesItem extends ArmorItem {
    private static final String VARIANT_TAG = "GlassesVariant";
    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            return 0;
        }

        @Override
        public int getDefenseForType(Type type) {
            return 1;
        }

        @Override
        public int getEnchantmentValue() {
            return 1;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_IRON;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return "bibliocraft:glasses";
        }

        @Override
        public float getToughness() {
            return 0;
        }

        @Override
        public float getKnockbackResistance() {
            return 0;
        }
    };

    public ReadingGlassesItem() {
        super(MATERIAL, Type.HELMET, new Properties().stacksTo(1));
    }

    public static Variant variant(ItemStack stack) {
        int id = stack.getTag() == null ? 0 : stack.getTag().getInt(VARIANT_TAG);
        return switch (id) {
            case 1 -> Variant.TINTED;
            case 2 -> Variant.MONOCLE;
            default -> Variant.READING;
        };
    }

    public static ItemStack withVariant(ItemStack stack, Variant variant) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.copyWithCount(1);
        if (variant == Variant.READING) {
            result.removeTagKey(VARIANT_TAG);
        } else {
            result.getOrCreateTag().putInt(VARIANT_TAG, variant.id());
        }
        return result;
    }

    public void addVariants(CreativeModeTab.Output output) {
        for (Variant variant : Variant.values()) {
            output.accept(withVariant(new ItemStack(this), variant));
        }
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        String base = super.getDescriptionId(stack);
        return switch (variant(stack)) {
            case TINTED -> base + ".tinted";
            case MONOCLE -> base + ".monocle";
            default -> base;
        };
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "bibliocraft:textures/armor/" + variant(stack).texture + ".png";
    }

    public enum Variant {
        READING(0, "glasses"),
        TINTED(1, "glasses_darktint"),
        MONOCLE(2, "glasses_mono");

        private final int id;
        private final String texture;

        Variant(int id, String texture) {
            this.id = id;
            this.texture = texture;
        }

        public int id() {
            return id;
        }
    }
}
