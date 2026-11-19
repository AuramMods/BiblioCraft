package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.book.PrintingLibrary;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.map.AtlasContents;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class PrintingPlateItem extends Item {
    public PrintingPlateItem() {
        super(new Properties().stacksTo(1));
    }

    public static ItemStack forBook(PrintingLibrary.Entry book) {
        ItemStack plate = new ItemStack(ModItems.PRINT_PLATE.get());
        CompoundTag tag = plate.getOrCreateTag();
        tag.putUUID("Book", book.id());
        tag.putString("Title", book.title());
        tag.putString("Author", book.author());
        return plate;
    }

    public static @Nullable UUID bookId(ItemStack plate) {
        CompoundTag tag = plate.getTag();
        return plate.is(ModItems.PRINT_PLATE.get()) && tag != null && tag.hasUUID("Book") ? tag.getUUID("Book") : null;
    }

    public static ItemStack forEnchantments(ItemStack book) {
        ItemStack plate = new ItemStack(ModItems.ENCHANTED_PLATE.get());
        plate.getOrCreateTag().put("BookTag", book.getOrCreateTag().copy());
        return plate;
    }

    public static ItemStack forAtlas(ItemStack source) {
        ItemStack atlas = source.copyWithCount(1);
        var snapshot = new AtlasContents(atlas);
        for (int slot = 0; slot < AtlasContents.SIZE; slot++) {
            if (!snapshot.isFilled(slot)) {
                snapshot.removeItemNoUpdate(slot);
            }
        }
        snapshot.select(snapshot.selected());
        snapshot.selectCompass(-1);
        snapshot.saveInventory();
        ItemStack plate = new ItemStack(ModItems.ATLAS_PLATE.get());
        plate.getOrCreateTag().put("AtlasTag", atlas.getOrCreateTag());
        return plate;
    }

    public static ItemStack atlasCopy(ItemStack plate) {
        var snapshot = plate.getTagElement("AtlasTag");
        if (!plate.is(ModItems.ATLAS_PLATE.get()) || snapshot == null) {
            return ItemStack.EMPTY;
        }
        var atlas = new ItemStack(ModItems.ATLAS_BOOK.get());
        atlas.setTag(snapshot.copy());
        return atlas;
    }

    public static ItemStack enchantedCopy(ItemStack plate) {
        var tag = plate.getTag();
        if (!plate.is(ModItems.ENCHANTED_PLATE.get()) || tag == null || remainingUses(plate) <= 0) {
            return ItemStack.EMPTY;
        }
        CompoundTag bookTag = tag.getCompound("BookTag");
        if (bookTag.getList("StoredEnchantments", Tag.TAG_COMPOUND).isEmpty()) {
            return ItemStack.EMPTY;
        }
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        book.setTag(bookTag.copy());
        return book;
    }

    public static int remainingUses(ItemStack stack) {
        var tag = stack.getTag();
        return Math.max(0, BiblioConfig.COMMON.enchPlateMaxUses.get() - (tag == null ? 0 : Math.max(0, tag.getInt("Uses"))));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.is(ModItems.ENCHANTED_PLATE.get()) || stack.is(ModItems.ATLAS_PLATE.get()) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        ItemStack enchanted = enchantedCopy(stack);
        ItemStack atlas = atlasCopy(stack);
        if (bookId(stack) != null) {
            tooltip.add(Component.literal(tag.getString("Title")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(tag.getString("Author")).withStyle(ChatFormatting.GRAY));
        } else if (!enchanted.isEmpty()) {
            Items.ENCHANTED_BOOK.appendHoverText(enchanted, level, tooltip, flag);
            tooltip.add(Component.translatable("item.bibliocraft.enchanted_plate.uses", remainingUses(stack))
                    .withStyle(ChatFormatting.GRAY));
        } else if (!atlas.isEmpty()) {
            tooltip.add(Component.translatable("item.bibliocraft.atlas_plate.sheets", new AtlasContents(atlas).filledMapCount())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.bibliocraft.print_plate.blank").withStyle(ChatFormatting.GRAY));
        }
    }
}
