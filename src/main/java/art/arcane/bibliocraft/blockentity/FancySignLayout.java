package art.arcane.bibliocraft.blockentity;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record FancySignLayout(List<Line> lines, Icon left, Icon right) {
    public static final String TAG = "SignLayout";
    public static final int LINE_COUNT = 15;
    public static final int MAX_TEXT_LENGTH = 200;
    private static final float[] TEXT_SIZES = {1, 1.5F, 2, 3, 4, 5};
    private static final int[] LINES_AT_SIZE = {16, 11, 7, 5, 4, 3};
    private static final Line EMPTY_LINE = new Line("", 1);
    private static final Icon DEFAULT_ICON = new Icon(1, 0, 0, 0);
    public static final FancySignLayout DEFAULT = new FancySignLayout(
            Collections.nCopies(LINE_COUNT, EMPTY_LINE), DEFAULT_ICON, DEFAULT_ICON);

    public FancySignLayout {
        if (lines.size() != LINE_COUNT) {
            throw new IllegalArgumentException("Expected " + LINE_COUNT + " sign lines");
        }
        lines = List.copyOf(lines);
    }

    public Icon icon(int slot) {
        return slot == 0 ? left : right;
    }

    public FancySignLayout mergeEdits(FancySignLayout opened, FancySignLayout edited) {
        List<Line> merged = new ArrayList<>(LINE_COUNT);
        for (int index = 0; index < LINE_COUNT; index++) {
            Line original = opened.lines.get(index);
            Line change = edited.lines.get(index);
            Line current = lines.get(index);
            merged.add(new Line(original.text.equals(change.text) ? current.text : change.text,
                    original.size == change.size ? current.size : change.size));
        }
        return new FancySignLayout(merged, left.mergeEdits(opened.left, edited.left),
                right.mergeEdits(opened.right, edited.right));
    }

    public void write(FriendlyByteBuf buffer) {
        for (Line line : lines) {
            buffer.writeUtf(line.text, MAX_TEXT_LENGTH);
            buffer.writeVarInt(line.size);
        }
        left.write(buffer);
        right.write(buffer);
    }

    public static FancySignLayout read(FriendlyByteBuf buffer) {
        List<Line> lines = new ArrayList<>(LINE_COUNT);
        for (int index = 0; index < LINE_COUNT; index++) {
            lines.add(new Line(buffer.readUtf(MAX_TEXT_LENGTH), buffer.readVarInt()));
        }
        return new FancySignLayout(lines, Icon.read(buffer), Icon.read(buffer));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag savedLines = new ListTag();
        for (Line line : lines) {
            CompoundTag saved = new CompoundTag();
            saved.putString("Text", line.text);
            saved.putInt("Size", line.size);
            savedLines.add(saved);
        }
        tag.put("Lines", savedLines);
        tag.put("Left", left.save());
        tag.put("Right", right.save());
        return tag;
    }

    public static FancySignLayout load(CompoundTag tag) {
        ListTag savedLines = tag.getList("Lines", Tag.TAG_COMPOUND);
        List<Line> lines = new ArrayList<>(LINE_COUNT);
        for (int index = 0; index < LINE_COUNT; index++) {
            if (index >= savedLines.size()) {
                lines.add(EMPTY_LINE);
            } else {
                CompoundTag saved = savedLines.getCompound(index);
                lines.add(new Line(saved.getString("Text"), saved.getInt("Size")));
            }
        }
        return new FancySignLayout(lines, Icon.load(tag.getCompound("Left")), Icon.load(tag.getCompound("Right")));
    }

    public record Line(String text, int size) {
        public Line {
            size = Mth.clamp(size, 0, TEXT_SIZES.length - 1);
            StringBuilder filtered = new StringBuilder(Math.min(MAX_TEXT_LENGTH, text.length()));
            for (int index = 0; index < text.length() && filtered.length() < MAX_TEXT_LENGTH; index++) {
                char character = text.charAt(index);
                if (Character.isHighSurrogate(character)) {
                    if (index + 1 < text.length() && filtered.length() + 2 <= MAX_TEXT_LENGTH
                            && Character.isLowSurrogate(text.charAt(index + 1))) {
                        filtered.append(character).append(text.charAt(++index));
                    }
                } else if (!Character.isLowSurrogate(character)
                        && (character == '§' || SharedConstants.isAllowedChatCharacter(character))) {
                    filtered.append(character);
                }
            }
            text = filtered.toString();
        }

        public float scale() {
            return TEXT_SIZES[size];
        }

        public int lineHeight() {
            return (int) (128.0F / LINES_AT_SIZE[size]);
        }
    }

    public record Icon(int size, int rotation, int x, int y) {
        public Icon {
            size = Mth.clamp(size, 0, 8);
            rotation = Mth.clamp(rotation, 0, 2);
            x = Mth.clamp(x, -116, 116);
            y = Mth.clamp(y, -62, 62);
        }

        private Icon mergeEdits(Icon opened, Icon edited) {
            return new Icon(opened.size == edited.size ? size : edited.size,
                    opened.rotation == edited.rotation ? rotation : edited.rotation,
                    opened.x == edited.x ? x : edited.x, opened.y == edited.y ? y : edited.y);
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(size).writeVarInt(rotation).writeVarInt(x).writeVarInt(y);
        }

        private static Icon read(FriendlyByteBuf buffer) {
            return new Icon(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Size", size);
            tag.putInt("Rotation", rotation);
            tag.putInt("X", x);
            tag.putInt("Y", y);
            return tag;
        }

        private static Icon load(CompoundTag tag) {
            return tag.isEmpty() ? DEFAULT_ICON
                    : new Icon(tag.getInt("Size"), tag.getInt("Rotation"), tag.getInt("X"), tag.getInt("Y"));
        }
    }
}
