package art.arcane.bibliocraft.storygen;

import net.minecraft.util.RandomSource;

final class StoryPlot {
    private final RandomSource random;
    private final String character1;
    private final String character2;
    private final int character1Gender;
    private final int character2Gender;
    private final String character1Job;
    private final String locationHome;
    private final String locationHomeType;
    private final String locationTraveledType;
    private final String nounBeasty;
    private final String nounBeastyName;

    StoryPlot(RandomSource randomm, String character1, String character2, int character1Gender, int character2Gender,
              String character1Job, String locationHome, String locationHomeType, String locationTraveledType) {
        this.random = randomm;
        this.character1 = character1;
        this.character2 = character2;
        this.character1Gender = character1Gender;
        this.character2Gender = character2Gender;
        this.character1Job = character1Job;
        this.locationHome = locationHome;
        this.locationHomeType = locationHomeType;
        this.locationTraveledType = locationTraveledType;
        nounBeasty = word(StoryWords.nounThing5);
        nounBeastyName = word(StoryWords.nounBeast);
    }

    private String word(String[] choices) {
        return choices[random.nextInt(choices.length)];
    }

    private static String possessive(int gender) {
        return gender == 0 ? "his" : "her";
    }

    private static String subject(int gender) {
        return gender == 0 ? "he" : "she";
    }

    private static String capitalizeSubject(int gender) {
        return gender == 0 ? "He" : "She";
    }

    private static String objectPronoun(int gender) {
        return gender == 0 ? "him" : "her";
    }

    String getStasis() {
        String sentence = word(StoryWords.capitalAdjectives1) + " into the " + word(StoryWords.nounLocation1)
                + " lies a " + word(StoryWords.adverb1) + " " + locationHomeType + " named "
                + locationHome + ". ";

        sentence += locationHome + " is known as the " + word(StoryWords.nounLocation2) + " "
                + word(StoryWords.preposition1) + " the " + word(StoryWords.verbyNoun1) + " to those "
                + word(StoryWords.verb1) + ". ";

        sentence += locationHome + " has " + word(StoryWords.adjective1) + " " + word(StoryWords.nounLocation3)
                + ", a " + word(StoryWords.adjective2) + " " + character1Job + " and a " + word(StoryWords.adjective3)
                + " " + word(StoryWords.nounLocation4) + ". ";

        sentence += "The " + word(StoryWords.adjective4) + " " + character1Job + " is " + word(StoryWords.adverb2)
                + " " + word(StoryWords.verb1) + " to " + word(StoryWords.verb2) + " " + word(StoryWords.phrase1)
                + ".";

        return sentence;
    }


    String getTrigger() {
        String sentence = "Each " + word(StoryWords.nounTime1) + ", " + character1Job + " "
                + character1 + " would " + word(StoryWords.verb3) + " to the " + word(StoryWords.adverb3)
                + " of the " + word(StoryWords.nounThing1) + " and " + word(StoryWords.verbPhrase1)
                + ". ";

        sentence += word(StoryWords.adverb4) + " the " + word(StoryWords.nounBigThing1) + " with "
                + possessive(character1Gender) + " " + word(StoryWords.verbPhrase2) + ",  "
                + subject(character1Gender) + " " + word(StoryWords.adverb5) + " " + possessive(character1Gender)
                + " " + word(StoryWords.verb4) + ". ";

        sentence += "A " + word(StoryWords.adverb6) + " " + word(StoryWords.verb5) + " " + word(StoryWords.adjective5)
                + " " + word(StoryWords.adverb7) + " " + possessive(character1Gender) + " "
                + word(StoryWords.nounThing2) + " and " + subject(character1Gender) + " " + word(StoryWords.verbPhrase3)
                + ". ";

        sentence += capitalizeSubject(character1Gender) + " could see a " + word(StoryWords.verb6)
                + " of " + word(StoryWords.nounThing3) + " " + word(StoryWords.adverb8) + " "
                + word(StoryWords.preposition1) + " the " + word(StoryWords.verbyNoun1) + ".";

        return sentence;
    }


    String getQuest() {
        String sentence = "The " + character1Job + " " + word(StoryWords.verbPhrase4) + " the "
                + word(StoryWords.adjective6) + " " + locationTraveledType + ". ";

        sentence += capitalizeSubject(character1Gender) + " " + word(StoryWords.adjective7)
                + " " + subject(character1Gender) + " would " + word(StoryWords.verb7) + " "
                + possessive(character1Gender) + " " + word(StoryWords.verb8) + " to the "
                + word(StoryWords.verbPhrase5) + " in " + locationHome + ". ";

        sentence += capitalizeSubject(character1Gender) + " " + word(StoryWords.verb9) + " they "
                + word(StoryWords.verb10) + " any " + word(StoryWords.nounThing4) + " they could "
                + word(StoryWords.adjective8) + " and " + subject(character1Gender) + " would "
                + word(StoryWords.verb11) + " a " + word(StoryWords.verb12) + " to the " + word(StoryWords.adjective6)
                + " " + locationTraveledType + " and " + word(StoryWords.verb13) + " to " + word(StoryWords.verb14)
                + ".";

        return sentence;
    }

    String getSurprise() {
        String sentence = word(StoryWords.conjunction1) + " " + word(StoryWords.adverb9) + " a "
                + word(StoryWords.adverb10) + " " + word(StoryWords.nounTime1) + " of " + word(StoryWords.verb15)
                + ", " + character1 + " could " + word(StoryWords.verb16) + " " + word(StoryWords.preposition2)
                + " the " + word(StoryWords.adjective9) + " " + word(StoryWords.verbyNoun1)
                + " and " + word(StoryWords.adverbPhrase1) + " the " + locationTraveledType
                + ". ";

        sentence += "This was " + word(StoryWords.phrase2) + " a " + word(StoryWords.adjective10)
                + " " + locationTraveledType + ", this was " + word(StoryWords.verbPhrase6)
                + ". ";

        sentence += "There was a " + word(StoryWords.adjective11) + " " + nounBeasty + " "
                + word(StoryWords.adverb11) + " on the " + word(StoryWords.nounThing6) + " of the "
                + locationTraveledType + " " + word(StoryWords.verb17) + " it " + word(StoryWords.verb18)
                + ".";

        return sentence;
    }

    String getChoice() {
        String sentence = character1 + " " + word(StoryWords.verb19) + " to " + word(StoryWords.verb20)
                + " if " + word(StoryWords.phrase3) + " but it was too " + word(StoryWords.adjective12)
                + " to see. ";

        sentence += character1 + " " + word(StoryWords.verb21) + " to " + word(StoryWords.phrase4)
                + " to " + word(StoryWords.verb22) + " " + word(StoryWords.phrase5) + ". ";

        sentence += "If the " + nounBeasty + " were to " + word(StoryWords.verb20) + " " + objectPronoun(character1Gender)
                + ", " + subject(character1Gender) + " " + word(StoryWords.verb23) + " get "
                + word(StoryWords.phrase6) + ". ";

        sentence += "But " + subject(character1Gender) + " " + word(StoryWords.verb23) + " not "
                + word(StoryWords.verb24) + " the " + word(StoryWords.nounThing7) + " of "
                + word(StoryWords.phrase7) + ".";

        return sentence;
    }

    String getClimax() {
        String sentence = "As " + character1 + " " + word(StoryWords.adverb12) + " the " + word(StoryWords.adjective13)
                + " " + locationTraveledType + ", " + subject(character1Gender) + " could "
                + word(StoryWords.verb26) + " a person " + word(StoryWords.verb27) + ". ";

        sentence += capitalizeSubject(character1Gender) + " " + word(StoryWords.phrase8) + " to "
                + word(StoryWords.verb28) + " to " + word(StoryWords.verb29) + " " + objectPronoun(character2Gender)
                + ". ";

        sentence += capitalizeSubject(character2Gender) + " was " + word(StoryWords.verb30)
                + " " + word(StoryWords.phrase9) + ". ";

        sentence += character1 + " " + word(StoryWords.verb31) + " the " + word(StoryWords.nounThing8)
                + " " + word(StoryWords.verb32) + " and " + word(StoryWords.verb33) + " if there was "
                + word(StoryWords.phrase10) + ". ";

        sentence += capitalizeSubject(character2Gender) + " said " + objectPronoun(character2Gender)
                + " name was " + character2 + ". ";

        sentence += "It was " + word(StoryWords.phrase12) + ".";

        return sentence;
    }

    String getReversal() {
        String sentence = character1 + " and " + character2 + " " + word(StoryWords.phrase13)
                + " the " + word(StoryWords.adjective13) + " " + word(StoryWords.nounThing9)
                + ", " + word(StoryWords.adjective14) + " to " + word(StoryWords.phrase14)
                + " to " + word(StoryWords.nounThing10) + ". ";

        sentence += "The " + nounBeasty + " had " + word(StoryWords.verb34) + " and " + word(StoryWords.phrase15)
                + ". ";

        sentence += "It was a " + nounBeastyName + "! ";

        sentence += "The " + nounBeastyName + " " + word(StoryWords.verb35) + " to try to "
                + word(StoryWords.phrase16) + ". ";

        sentence += character1 + ", " + word(StoryWords.adjective14) + " to " + word(StoryWords.verb36)
                + " " + character2 + ", " + word(StoryWords.phrase17) + " and " + word(StoryWords.verb37)
                + " at the " + nounBeastyName + ".";

        return sentence;
    }


    String getResolution() {
        String sentence = "Just as the " + nounBeastyName + " " + word(StoryWords.verb19) + " to "
                + word(StoryWords.verb38) + " " + character1 + ", " + subject(character1Gender)
                + " " + word(StoryWords.verb39) + " " + possessive(character1Gender) + " "
                + word(StoryWords.nounThing11) + " deep into the " + nounBeastyName + "s' "
                + word(StoryWords.nounThing12) + ", " + word(StoryWords.phrase18) + ". ";

        sentence += character2 + " was " + word(StoryWords.verb40) + " at " + word(StoryWords.phrase19)
                + ". ";

        sentence += character1 + " " + word(StoryWords.phrase20) + " " + character2 + " and "
                + word(StoryWords.verb41) + " " + objectPronoun(character2Gender) + ", reminding "
                + objectPronoun(character2Gender) + " that " + word(StoryWords.phrase21) + ". ";

        sentence += "They could " + word(StoryWords.phrase22) + " to " + locationHome + " now.";
        return sentence;
    }
}
