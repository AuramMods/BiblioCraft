package art.arcane.bibliocraft.book;

import art.arcane.bibliocraft.catalog.CatalogContents;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class SlottedBookContents {
    public static final int LINES = 5;
    public static final int TEXT_LENGTH = 20;

    private SlottedBookContents() {}

    public static ItemStack stored(ItemStack book) {
        if (book.getTag() != null) {
            ListTag inventory = book.getTag().getList("Inventory", Tag.TAG_COMPOUND);
            for (int index = 0; index < inventory.size(); index++) {
                CompoundTag item = inventory.getCompound(index);
                if (item.contains("Slot", Tag.TAG_ANY_NUMERIC) && item.getInt("Slot") == 0) {
                    return ItemStack.of(item);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static void store(ItemStack book, ItemStack stack) {
        ListTag inventory = new ListTag();
        if (!stack.isEmpty()) {
            CompoundTag saved = stack.save(new CompoundTag());
            saved.putByte("Slot", (byte) 0);
            inventory.add(saved);
        }
        book.getOrCreateTag().put("Inventory", inventory);
    }

    public static Label label(ItemStack book) {
        CompoundTag saved = book.getTagElement("lines");
        var lines = new ArrayList<String>(LINES);
        for (int i = 1; i <= LINES; i++) {
            lines.add(saved == null ? "" : saved.getString("line" + i));
        }
        return new Label(book.hasCustomHoverName() ? book.getHoverName().getString() : "", lines);
    }

    private static String line(String text) {
        return CatalogContents.bounded(SharedConstants.filterText(text), TEXT_LENGTH);
    }

    public record Label(String title, List<String> lines) {
        public Label {
            if (lines.size() != LINES) {
                throw new IllegalArgumentException("A slotted book has five description lines");
            }
            title = line(title).strip();
            lines = lines.stream().map(SlottedBookContents::line).toList();
        }

        public void save(ItemStack book) {
            if (title.isEmpty()) {
                book.resetHoverName();
            } else {
                book.setHoverName(Component.literal(title));
            }
            var saved = new CompoundTag();
            for (int i = 0; i < LINES; i++) {
                saved.putString("line" + (i + 1), lines.get(i));
            }
            book.getOrCreateTag().put("lines", saved);
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeUtf(title, TEXT_LENGTH);
            for (String line : lines) {
                buffer.writeUtf(line, TEXT_LENGTH);
            }
        }

        public static Label read(FriendlyByteBuf buffer) {
            String title = buffer.readUtf(TEXT_LENGTH);
            var lines = new ArrayList<String>(LINES);
            for (int i = 0; i < LINES; i++) {
                lines.add(buffer.readUtf(TEXT_LENGTH));
            }
            return new Label(title, lines);
        }
    }
}
