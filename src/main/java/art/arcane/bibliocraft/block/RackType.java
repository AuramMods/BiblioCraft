package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.config.BiblioConfig;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

import java.util.List;
import java.util.Locale;

public enum RackType {
    POTIONS(4, 3, 18, 19, "potion_shelf_contents", "potionshelfgui"),
    TOOLS(2, 2, 54, 38, "tool_rack_contents", "toolrackgui");

    private static final List<String> POTION_NAMES = List.of(
            "potion", "flask", "drink", "elixir", "jellyitem", "milkshakeitem", "nutellaitem", "vegemiteitem",
            "sauceitem", "juiceitem", "smoothieitem", "wateritem", "sodaitem", "brewitem", "butteritem", "milkitem",
            "dressingitem", "coffee", "espressoitem", "oilitem", "ketchup", "syrupitem", "chutneyitem", "teaitem",
            "vinegaritem", "eggnog", "lemonaide", "hotchocolate", "mustard", "yogurtitem", "creamitem");
    private static final List<String> TOOL_NAMES = List.of(
            "tconstruct.items.tools", "item.tool", "hammer", "wrench", "scythe", "staff", "chisel", "maptool",
            "hand_drill", "handdrill", "dagger", "metagenerated_tool", "gt.metatool", "item.infitool",
            "cleaver", "hatchet", "mattock", "excavator", "frypan", "battlesign", "grapple", "chemthrower",
            "revolver", "rapier", "shortbow", "shuriken", "crossbow", "arrow", "tconstruct.bolt", "psi:cad",
            "vector_ruler", "saw");

    private final int columns;
    private final int rows;
    private final int slotSpacingX;
    private final int slotSpacingY;
    private final TagKey<Item> contentTag;
    private final ResourceLocation screenTexture;

    RackType(int columns, int rows, int slotSpacingX, int slotSpacingY, String contentTag, String screenTexture) {
        this.columns = columns;
        this.rows = rows;
        this.slotSpacingX = slotSpacingX;
        this.slotSpacingY = slotSpacingY;
        this.contentTag = ItemTags.create(ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, contentTag));
        this.screenTexture = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID,
                "textures/gui/" + screenTexture + ".png");
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    public int size() {
        return columns * rows;
    }

    public int menuX(int slot) {
        return 53 + slot % columns * slotSpacingX;
    }

    public int menuY(int slot) {
        return 15 + slot / columns * slotSpacingY;
    }

    public ResourceLocation screenTexture() {
        return screenTexture;
    }

    public Direction opening(Direction facing) {
        return this == POTIONS ? facing.getCounterClockWise() : facing.getClockWise();
    }

    public Direction facingForOpening(Direction opening) {
        return this == POTIONS ? opening.getClockWise() : opening.getCounterClockWise();
    }

    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(contentTag)) {
            return true;
        }
        Item item = stack.getItem();
        String id = stack.getDescriptionId().toLowerCase(Locale.ROOT);
        String name = item.getName(stack).getString().toLowerCase(Locale.ROOT);
        if (this == POTIONS) {
            return !id.contains("shelf") && (item instanceof PotionItem
                    || POTION_NAMES.stream().anyMatch(word -> id.contains(word) || name.contains(word))
                    || matchesConfiguredName(id, name, BiblioConfig.COMMON.additionalPotions.get()));
        }
        return item instanceof TieredItem || item instanceof ProjectileWeaponItem || item instanceof TridentItem
                || item instanceof ArrowItem || item instanceof FishingRodItem || item instanceof ShearsItem
                || item instanceof FlintAndSteelItem || TOOL_NAMES.stream().anyMatch(id::contains)
                || matchesConfiguredName(id, name, BiblioConfig.COMMON.additionalTools.get());
    }

    private static boolean matchesConfiguredName(String id, String name, String allowedNames) {
        for (String allowed : allowedNames.split(",")) {
            String keyword = allowed.strip().toLowerCase(Locale.ROOT);
            if (!keyword.isEmpty() && (id.contains(keyword) || name.contains(keyword))) {
                return true;
            }
        }
        return false;
    }
}
