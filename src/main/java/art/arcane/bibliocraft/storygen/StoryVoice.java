package art.arcane.bibliocraft.storygen;

import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

public enum StoryVoice {
    STANDARD(null, null, null),
    PIG(new String[]{"Oink", "Oinky"},
            new String[]{"snort", "oink", "oink", "oiiink", "weeeet", "squeal", "oink"},
            new String[]{"mud", "bacon", "ham", "carrot on a stick", "pig"}),
    CHICKEN(new String[]{"Cluck", "Baguck"},
            new String[]{"cluck", "beguck", "bruck", "cluck", "cock a doodle doo"},
            new String[]{"chicken", "wing", "breast", "egg", "rooster", "hen"}),
    COW(new String[]{"Moo", "Mooo"}, new String[]{"moo", "mooo", "moam", "moooo"},
            new String[]{"cow", "steak", "milk"}),
    SHEEP(new String[]{"Maa", "Baa"}, new String[]{"maa", "baa", "maaaa", "baaaa"},
            new String[]{"sheep", "shears", "wool", "mutton"}),
    WOLF(new String[]{"Woof", "Bark", "Ruff"},
            new String[]{"woof", "bark", "ruff", "awhoooo", "yip yip", "wooof"}, new String[]{"dog", "wolf"}),
    OCELOT(new String[]{"Meow", "Mew"},
            new String[]{"meow", "mew", "meeeoow", "mew mew", "hisssss", "rreraow"},
            new String[]{"cat", "ocelot", "fish"}),
    CREEPER(new String[]{"Sssss", "Psssss"}, new String[]{"sssss", "sssssss", "pssss", "hsssss"},
            new String[]{"creeper", "tnt", "explosion", "dynamite"}),
    ZOMBIE(new String[]{"Meeehhh", "Eeeehhhh"}, new String[]{"mehhhaaa", "eeehhhahhh", "ehh", "ahh", "mehh"},
            new String[]{"brains"}),
    ENDERMAN(new String[]{"Shhh", "Huuuahhhh"}, new String[]{"shhhh", "darkness", "will", "fall", "silence"},
            new String[]{"ender", "dragon", "silence", "end"});

    private static final Map<EntityType<?>, StoryVoice> MOB_VOICES = Map.ofEntries(
            entry(EntityType.PIG, PIG), entry(EntityType.ZOMBIFIED_PIGLIN, PIG), entry(EntityType.CHICKEN, CHICKEN),
            entry(EntityType.COW, COW), entry(EntityType.MOOSHROOM, COW), entry(EntityType.SHEEP, SHEEP),
            entry(EntityType.WOLF, WOLF), entry(EntityType.OCELOT, OCELOT), entry(EntityType.CREEPER, CREEPER),
            entry(EntityType.ZOMBIE, ZOMBIE), entry(EntityType.HUSK, ZOMBIE), entry(EntityType.ZOMBIE_VILLAGER, ZOMBIE),
            entry(EntityType.DROWNED, ZOMBIE), entry(EntityType.ENDERMAN, ENDERMAN));
    private static final Set<String[]> NOUNS = Set.of(StoryWords.nounLocation3, StoryWords.nounLocation4,
            StoryWords.nounTime1, StoryWords.nounThing1, StoryWords.nounBigThing1, StoryWords.nounThing2,
            StoryWords.nounThing3, StoryWords.nounThing4, StoryWords.nounThing5, StoryWords.nounThing7,
            StoryWords.nounThing8, StoryWords.nounThing9, StoryWords.nounThing10, StoryWords.nounBeast,
            StoryWords.nounThing11, StoryWords.nounThing12);

    private final String[] opening;
    private final String[] noises;
    private final String[] nouns;

    StoryVoice(String[] opening, String[] noises, String[] nouns) {
        this.opening = opening;
        this.noises = noises;
        this.nouns = nouns;
    }

    public static StoryVoice forMob(EntityType<?> type) {
        return MOB_VOICES.getOrDefault(type, STANDARD);
    }

    public static StoryVoice fromName(String name) {
        for (StoryVoice voice : values()) {
            if (voice.name().equals(name)) {
                return voice;
            }
        }
        return STANDARD;
    }

    String[] words(String[] defaults) {
        if (this == STANDARD) {
            return defaults;
        }
        if (defaults == StoryWords.capitalAdjectives1) {
            return opening;
        }
        return NOUNS.contains(defaults) ? nouns : noises;
    }
}
