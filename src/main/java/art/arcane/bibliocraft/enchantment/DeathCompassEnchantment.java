package art.arcane.bibliocraft.enchantment;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.AtlasItem;
import art.arcane.bibliocraft.registry.ModEnchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class DeathCompassEnchantment extends Enchantment {
    private static final EnchantmentCategory ATLAS = EnchantmentCategory.create("bibliocraft_atlas", item -> item instanceof AtlasItem);
    private static final String LEGACY_MARKER = "bibliocraft:death_compass_enabled";

    public DeathCompassEnchantment() {
        super(Rarity.VERY_RARE, ATLAS, new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    public static boolean enabled() {
        return BiblioConfig.COMMON.enableAtlas.get() && BiblioConfig.COMMON.enableDeathCompass.get();
    }

    @Override
    public int getMinCost(int level) {
        return 10;
    }

    @Override
    public int getMaxCost(int level) {
        return 60;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return enabled() && super.canEnchant(stack);
    }

    @Override
    public boolean isDiscoverable() {
        return enabled();
    }

    @Override
    public boolean isTradeable() {
        return enabled();
    }

    public static boolean tracksDeath(ItemStack stack) {
        return stack.getItem() instanceof AtlasItem
                && (hasLegacyMarker(stack) || EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.DEATH_COMPASS.get(), stack) > 0);
    }

    public static boolean hasLegacyMarker(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(LEGACY_MARKER);
    }

    public static ItemStack apply(ItemStack atlas) {
        var enchantment = ModEnchantments.DEATH_COMPASS.get();
        if (EnchantmentHelper.getItemEnchantmentLevel(enchantment, atlas) == 0) {
            atlas.enchant(enchantment, 1);
        }
        atlas.removeTagKey(LEGACY_MARKER);
        return atlas;
    }
}
