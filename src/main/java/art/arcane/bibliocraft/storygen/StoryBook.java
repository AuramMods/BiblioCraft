package art.arcane.bibliocraft.storygen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class StoryBook {
    private static final String[] PROFESSIONS = {
            "blacksmith", "carpenter", "chef", "geologist", "miner", "reverend", "protector"};
    private static final String[] LOCATIONS = {"village", "camp", "castle", "house", "cottage", "city"};
    private static final String[] TITLE_PREFIXES = {
            "Dragon", "Witch", "Prized", "Dream", "Emperor", "Flower", "Dark", "The", "Secret", "Ravaged",
            "Silent", "Shadow", "Wet", "Angel", "Life", "Night", "World", "Servent", "Gate of", "Dying",
            "Burning", "Moon", "Petal", "Rough", "Ship of", "Last", "Deep", "Shadow of", "Healing", "Princess",
            "Star", "Truth of", "Seventh", "Sharp", "Vanilla", "Tower of", "Bad", "Bold", "Vacant", "Snow",
            "Jungle", "Ancient", "Frozen", "Whispering", "Wizards", "Living", "Wool", "Cactus", "Self",
            "Butterfly", "Delicious", "Family", "Short", "Long", "Rooftop", "Window", "Day", "Simple", "Red",
            "Autumn", "Last", "Hot", "War", "Micro"};
    private static final String[] TITLE_SUFFIXES = {
            "Slayer", "Mage", "Touch", "Wind", "Wizard", "Memory", "Voyage", "Rose", "Heat", "Illusion",
            "Obsession", "Sword", "Pickaxe", "Miner", "Danger", "Truth", "Prophecy", "Lords", "Souls",
            "Secrets", "Words", "Hunter", "Abyss", "Bridge", "Names", "Flames", "Dreams", "Storm", "Serpents",
            "Consort", "Stones", "Silk", "Shards", "Sky", "Wolf", "Academy", "Rainbows", "Planet", "Healing",
            "Past", "Memory", "School", "Ice", "Flame", "Thoughts", "Years", "Waves", "Destruction", "Vision",
            "Treats", "Wheat", "Guy", "Distance", "Concern", "Adventure", "Journey", "Dawn", "Dynamite",
            "Darkness", "Giant", "Titans", "Fuzz", "Games"};
    private static final String CAPITALS = "ABCDFGHJKLMNPQRSTVWZ";
    private static final String VOWELS = "aeiou";
    private static final String[] CONSONANTS = {
            "b", "c", "d", "f", "g", "k", "l", "m", "n", "p", "r", "s", "t", "v", "w",
            "rd", "rk", "rv", "rt", "nk", "wn", "th", "ch", "lb", "tr", "mw"};
    private static final String[] ENDINGS = {
            "c", "d", "g", "k", "l", "m", "n", "p", "r", "s", "t", "rd", "rk", "rv", "rt", "ny",
            "nk", "wn", "ch", "gon", "wood", "ix", "th", "ck"};

    private StoryBook() {}

    public static ItemStack create(RandomSource random) {
        StoryPlot story = new StoryPlot(random, name(random), name(random), random.nextInt(2), random.nextInt(2),
                choose(random, PROFESSIONS), name(random), choose(random, LOCATIONS), choose(random, LOCATIONS));
        ListTag pages = new ListTag();
        for (String page : List.of(story.getStasis(), story.getTrigger(), story.getQuest(), story.getSurprise(),
                story.getChoice(), story.getClimax(), story.getReversal(), story.getResolution())) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(page))));
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", choose(random, TITLE_PREFIXES) + " " + choose(random, TITLE_SUFFIXES));
        tag.putString("author", name(random));
        tag.putBoolean("resolved", true);
        tag.put("pages", pages);
        return book;
    }

    private static String name(RandomSource random) {
        String first = String.valueOf(CAPITALS.charAt(random.nextInt(CAPITALS.length()))) + vowel(random);
        return switch (random.nextInt(3)) {
            case 0 -> first + choose(random, CONSONANTS) + vowel(random) + vowel(random);
            case 1 -> first + vowel(random) + choose(random, ENDINGS);
            default -> first + choose(random, CONSONANTS) + vowel(random) + choose(random, ENDINGS);
        };
    }

    private static char vowel(RandomSource random) {
        return VOWELS.charAt(random.nextInt(VOWELS.length()));
    }

    private static String choose(RandomSource random, String[] words) {
        return words[random.nextInt(words.length)];
    }
}
