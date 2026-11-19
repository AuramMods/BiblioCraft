package art.arcane.bibliocraft.book;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClipboardContent {
    public static final int MAX_PAGES = 50;
    public static final int TASKS = 9;
    public static final int TITLE_LENGTH = 26;
    public static final int TASK_LENGTH = 23;
    private static final Task EMPTY_TASK = new Task("", 0);
    public static final Page EMPTY_PAGE = new Page("", Collections.nCopies(TASKS, EMPTY_TASK));

    private ClipboardContent() {}

    public static int count(ItemStack stack) {
        return stack.getTag() == null ? 1 : Mth.clamp(stack.getTag().getInt("totalPages"), 1, MAX_PAGES);
    }

    public static int current(ItemStack stack) {
        return stack.getTag() == null ? 0 : Mth.clamp(stack.getTag().getInt("currentPage"), 1, count(stack)) - 1;
    }

    public static Page page(ItemStack stack, int index) {
        if (stack.getTag() == null || index < 0 || index >= count(stack)) {
            return EMPTY_PAGE;
        }
        CompoundTag page = stack.getTag().getCompound("page" + (index + 1));
        CompoundTag text = page.getCompound("tasks");
        int[] states = page.getIntArray("taskStates");
        var tasks = new ArrayList<Task>(TASKS);
        for (int i = 0; i < TASKS; i++) {
            tasks.add(new Task(text.getString("task" + (i + 1)), i < states.length ? states[i] : 0));
        }
        return new Page(page.getString("title"), tasks);
    }

    public static void select(ItemStack stack, int index) {
        if (index >= 0 && index < count(stack)) {
            stack.getOrCreateTag().putInt("currentPage", index + 1);
        }
    }

    public static void save(ItemStack stack, int index, Page page) {
        int count = count(stack);
        if (index < 0 || index > count || index >= MAX_PAGES) {
            return;
        }
        var output = new CompoundTag();
        var text = new CompoundTag();
        int[] states = new int[TASKS];
        for (int i = 0; i < TASKS; i++) {
            Task task = page.tasks().get(i);
            text.putString("task" + (i + 1), task.text());
            states[i] = task.state();
        }
        output.putString("title", page.title());
        output.put("tasks", text);
        output.putIntArray("taskStates", states);
        CompoundTag root = stack.getOrCreateTag();
        root.put("page" + (index + 1), output);
        root.putInt("totalPages", Math.max(index + 1, count));
    }

    private static String line(String text, int limit) {
        String filtered = net.minecraft.SharedConstants.filterText(text);
        int end = Math.min(filtered.length(), limit);
        if (end < filtered.length() && end > 0 && Character.isHighSurrogate(filtered.charAt(end - 1))) {
            end--;
        }
        return filtered.substring(0, end);
    }

    public record Task(String text, int state) {
        public Task {
            text = line(text, TASK_LENGTH);
            state = Mth.clamp(state, 0, 2);
        }

        public Task cycle() {
            return new Task(text, (state + 1) % 3);
        }
    }

    public record Page(String title, List<Task> tasks) {
        public Page {
            title = line(title, TITLE_LENGTH);
            if (tasks.size() != TASKS) {
                throw new IllegalArgumentException("A clipboard page has nine tasks");
            }
            tasks = List.copyOf(tasks);
        }

        public Page cycle(int index) {
            var changed = new ArrayList<>(tasks);
            changed.set(index, changed.get(index).cycle());
            return new Page(title, changed);
        }

        public Page merge(Page before, Page desired) {
            var merged = new ArrayList<Task>(TASKS);
            for (int index = 0; index < TASKS; index++) {
                Task base = before.tasks.get(index);
                Task edited = desired.tasks.get(index);
                Task live = tasks.get(index);
                merged.add(new Task(base.text.equals(edited.text) ? live.text : edited.text,
                        base.state == edited.state ? live.state : edited.state));
            }
            return new Page(before.title.equals(desired.title) ? title : desired.title, merged);
        }

        public int visibleParts() {
            int mask = 0;
            for (int i = 0; i < TASKS; i++) {
                int state = tasks.get(i).state();
                if (state > 0) {
                    mask |= 1 << (2 * i + state - 1);
                }
            }
            return mask;
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeUtf(title, TITLE_LENGTH);
            for (Task task : tasks) {
                buffer.writeUtf(task.text(), TASK_LENGTH);
                buffer.writeByte(task.state());
            }
        }

        public static Page read(FriendlyByteBuf buffer) {
            String title = buffer.readUtf(TITLE_LENGTH);
            var tasks = new ArrayList<Task>(TASKS);
            for (int i = 0; i < TASKS; i++) {
                tasks.add(new Task(buffer.readUtf(TASK_LENGTH), buffer.readUnsignedByte()));
            }
            return new Page(title, tasks);
        }
    }

    public record View(int current, int count, Page page) {
        public static View of(ItemStack stack) {
            int current = ClipboardContent.current(stack);
            return new View(current, ClipboardContent.count(stack), ClipboardContent.page(stack, current));
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(current);
            buffer.writeVarInt(count);
            page.write(buffer);
        }

        public static View read(FriendlyByteBuf buffer) {
            return new View(buffer.readVarInt(), buffer.readVarInt(), Page.read(buffer));
        }
    }
}
