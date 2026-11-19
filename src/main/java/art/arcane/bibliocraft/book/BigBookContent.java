package art.arcane.bibliocraft.book;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class BigBookContent {
    public static final int MAX_PAGES = 256;
    public static final int LINES = 44;
    public static final int LINE_LENGTH = 200;
    public static final int CHAPTERS = 8;
    public static final int LABEL_LENGTH = 15;
    public static final int TITLE_LENGTH = 34;
    public static final int AUTHOR_LENGTH = 64;
    private static final float[] SCALES = {0.5F, 0.65F, 0.8F, 1, 1.2F, 1.4F, 1.75F, 2};

    private BigBookContent() {}

    public static Header header(ItemStack book, int defaultSize) {
        CompoundTag tag = book.getTag();
        if (tag == null) {
            return new Header(1, 0, false, "", "", defaultSize,
                    Collections.nCopies(CHAPTERS, Chapter.EMPTY));
        }
        CompoundTag chapterTag = tag.getCompound("chapters");
        int[] enabled = chapterTag.getIntArray("chapBools");
        int[] pages = chapterTag.getIntArray("chapPages");
        int count = Mth.clamp(tag.getInt("pagesTotal"), 1, MAX_PAGES);
        List<Chapter> chapters = new ArrayList<>(CHAPTERS);
        for (int index = 0; index < CHAPTERS; index++) {
            int page = index < pages.length ? Mth.clamp(pages[index], 0, count - 1) : 0;
            chapters.add(new Chapter(index < enabled.length && enabled[index] == 1, page,
                    chapterTag.getString("chapline" + (2 * index)), chapterTag.getString("chapline" + (2 * index + 1))));
        }
        String title = tag.getString("title");
        if (title.isEmpty() && tag.getBoolean("signed")) {
            String name = tag.getCompound("display").getString("Name");
            try {
                Component component = Component.Serializer.fromJson(name);
                title = component == null ? "" : ChatFormatting.stripFormatting(component.getString());
            } catch (com.google.gson.JsonParseException ignored) {
                title = ChatFormatting.stripFormatting(name);
            }
        }
        return new Header(count, tag.getInt("pagesCurrent"), tag.getBoolean("signed"), title,
                tag.getString("author"), defaultSize, chapters);
    }

    public static int bookmark(ItemStack book) {
        CompoundTag tag = book.getTag();
        return tag == null ? 0 : Mth.clamp(tag.getInt("pagesCurrent"), 0,
                Mth.clamp(tag.getInt("pagesTotal"), 1, MAX_PAGES) - 1);
    }

    public static boolean signed(ItemStack book) {
        return book.hasTag() && book.getTag().getBoolean("signed");
    }

    public static Page page(ItemStack book, int index, int defaultSize) {
        var tag = book.getTag();
        if (tag == null) {
            return Page.blank(defaultSize);
        }
        CompoundTag pages = tag.getCompound("pages");
        ListTag text = pages.getList("page" + index, Tag.TAG_STRING);
        int[] sizes = pages.getIntArray("pageScale" + index);
        List<Line> lines = new ArrayList<>(LINES);
        for (int row = 0; row < LINES; row++) {
            lines.add(new Line(text.getString(row), row < sizes.length ? sizes[row] : defaultSize));
        }
        return new Page(lines);
    }

    public static void update(ItemStack book, Header header, Map<Integer, Page> edits, boolean rewriteChapters) {
        var tag = book.getOrCreateTag();
        tag.putInt("pagesTotal", header.pages());
        tag.putInt("pagesCurrent", header.bookmark());
        if (!edits.isEmpty()) {
            CompoundTag pages = tag.getCompound("pages");
            edits.forEach((index, page) -> {
                var lines = new ListTag();
                int[] sizes = new int[LINES];
                for (int row = 0; row < LINES; row++) {
                    Line line = page.lines().get(row);
                    lines.add(StringTag.valueOf(line.text()));
                    sizes[row] = line.size();
                }
                pages.put("page" + index, lines);
                pages.putIntArray("pageScale" + index, sizes);
            });
            tag.put("pages", pages);
        }
        if (rewriteChapters) {
            CompoundTag chapters = tag.getCompound("chapters");
            int[] enabled = new int[CHAPTERS];
            int[] targets = new int[CHAPTERS];
            for (int index = 0; index < CHAPTERS; index++) {
                Chapter chapter = header.chapters().get(index);
                enabled[index] = chapter.enabled() ? 1 : 0;
                targets[index] = chapter.page();
                chapters.putString("chapline" + (2 * index), chapter.first());
                chapters.putString("chapline" + (2 * index + 1), chapter.second());
            }
            chapters.putIntArray("chapBools", enabled);
            chapters.putIntArray("chapPages", targets);
            tag.put("chapters", chapters);
        }
        if (header.signed() && (!signed(book) || (tag.getString("title").isEmpty() && !header.title().isEmpty()))) {
            tag.putBoolean("signed", true);
            tag.putString("title", header.title());
            tag.putString("author", header.author());
            book.setHoverName(Component.literal(header.title()));
        }
    }

    public static void stripPageData(CompoundTag tag) {
        tag.remove("pages");
        tag.remove("chapters");
    }

    private static String limit(String text, int length, boolean allowFormatting) {
        StringBuilder filtered = new StringBuilder(Math.min(length, text.length()));
        for (int index = 0; index < text.length() && filtered.length() < length; index++) {
            char character = text.charAt(index);
            if (Character.isHighSurrogate(character)) {
                if (index + 1 < text.length() && filtered.length() + 2 <= length
                        && Character.isLowSurrogate(text.charAt(index + 1))) {
                    filtered.append(character).append(text.charAt(++index));
                }
            } else if (allowFormatting && character == '§' && index + 1 < text.length()
                    && filtered.length() + 2 <= length && ChatFormatting.getByCode(text.charAt(index + 1)) != null) {
                filtered.append(character).append(text.charAt(++index));
            } else if (!Character.isLowSurrogate(character) && SharedConstants.isAllowedChatCharacter(character)) {
                filtered.append(character);
            }
        }
        return filtered.toString();
    }

    public static String validTitle(String title) {
        return limit(title, TITLE_LENGTH, false).strip();
    }

    public record Line(String text, int size) {
        public Line {
            text = limit(text, LINE_LENGTH, true);
            size = Mth.clamp(size, 0, SCALES.length - 1);
        }

        public float scale() {
            return SCALES[size];
        }

        public int height() {
            return Math.round(10 * scale());
        }
    }

    public record Page(List<Line> lines) {
        public Page {
            if (lines.size() != LINES) {
                throw new IllegalArgumentException("Expected " + LINES + " book lines");
            }
            lines = List.copyOf(lines);
        }

        public static Page blank(int defaultSize) {
            return new Page(Collections.nCopies(LINES, new Line("", defaultSize)));
        }

        public Page merge(Page opened, Page edited) {
            List<Line> result = new ArrayList<>(LINES);
            for (int row = 0; row < LINES; row++) {
                Line before = opened.lines.get(row);
                Line after = edited.lines.get(row);
                Line current = lines.get(row);
                result.add(new Line(before.text().equals(after.text()) ? current.text() : after.text(),
                        before.size() == after.size() ? current.size() : after.size()));
            }
            return new Page(result);
        }

        public void write(FriendlyByteBuf buffer) {
            for (Line line : lines) {
                buffer.writeUtf(line.text(), LINE_LENGTH).writeByte(line.size());
            }
        }

        public static Page read(FriendlyByteBuf buffer) {
            List<Line> lines = new ArrayList<>(LINES);
            for (int row = 0; row < LINES; row++) {
                lines.add(new Line(buffer.readUtf(LINE_LENGTH), buffer.readUnsignedByte()));
            }
            return new Page(lines);
        }
    }

    public record Chapter(boolean enabled, int page, String first, String second) {
        public static final Chapter EMPTY = new Chapter(false, 0, "", "");

        public Chapter {
            page = Mth.clamp(page, 0, MAX_PAGES - 1);
            first = limit(first, LABEL_LENGTH, false);
            second = limit(second, LABEL_LENGTH, false);
        }

        public Chapter inBook(int count) {
            return page >= count ? new Chapter(enabled, count - 1, first, second) : this;
        }

        public Chapter merge(Chapter opened, Chapter edited) {
            return new Chapter(opened.enabled == edited.enabled ? enabled : edited.enabled,
                    opened.page == edited.page ? page : edited.page,
                    opened.first.equals(edited.first) ? first : edited.first,
                    opened.second.equals(edited.second) ? second : edited.second);
        }

        public static void writeAll(FriendlyByteBuf buffer, List<Chapter> chapters) {
            for (Chapter chapter : chapters) {
                buffer.writeBoolean(chapter.enabled()).writeByte(chapter.page());
                buffer.writeUtf(chapter.first(), LABEL_LENGTH);
                buffer.writeUtf(chapter.second(), LABEL_LENGTH);
            }
        }

        public static List<Chapter> readAll(FriendlyByteBuf buffer) {
            List<Chapter> chapters = new ArrayList<>(CHAPTERS);
            for (int index = 0; index < CHAPTERS; index++) {
                chapters.add(new Chapter(buffer.readBoolean(), buffer.readUnsignedByte(),
                        buffer.readUtf(LABEL_LENGTH), buffer.readUtf(LABEL_LENGTH)));
            }
            return chapters;
        }
    }

    public record Header(int pages, int bookmark, boolean signed, String title, String author, int defaultSize,
                         List<Chapter> chapters) {
        public Header {
            pages = Mth.clamp(pages, 1, MAX_PAGES);
            bookmark = Mth.clamp(bookmark, 0, pages - 1);
            defaultSize = Mth.clamp(defaultSize, 0, SCALES.length - 1);
            title = validTitle(title);
            author = limit(author, AUTHOR_LENGTH, false);
            if (chapters.size() != CHAPTERS) {
                throw new IllegalArgumentException("Expected " + CHAPTERS + " book chapters");
            }
            int count = pages;
            chapters = chapters.stream().map(chapter -> chapter.inBook(count)).toList();
        }

        public Header withBookmark(int selected) {
            return new Header(pages, selected, signed, title, author, defaultSize, chapters);
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(pages).writeByte(bookmark).writeBoolean(signed);
            buffer.writeUtf(title, TITLE_LENGTH).writeUtf(author, AUTHOR_LENGTH).writeByte(defaultSize);
            Chapter.writeAll(buffer, chapters);
        }

        public static Header read(FriendlyByteBuf buffer) {
            return new Header(buffer.readVarInt(), buffer.readUnsignedByte(), buffer.readBoolean(),
                    buffer.readUtf(TITLE_LENGTH), buffer.readUtf(AUTHOR_LENGTH), buffer.readUnsignedByte(),
                    Chapter.readAll(buffer));
        }
    }
}
