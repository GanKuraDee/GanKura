package com.deeply.gankura.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Sea Creature を釣り上げたときにチャットへ出る文言と、その種類。
 *
 * SkyHanni が使っている SkyHanni-REPO の constants/SeaCreatures.json から起こしたもの。
 * 色コードを落とした文言をそのまま鍵にして引く。
 * 部分一致にすると誰かの発言や別の文言に巻き込まれるため、丸ごと一致で見る。
 *
 * scratchpad/generate_sea_creature_catches.py が生成するので、手では書き換えない。
 */
public final class SeaCreatureCatches {

    /**
     * 釣り上げた Sea Creature。
     *
     * name は色なしの名前、displayName はレア度の色付きで、
     * レアなものは太字も付く
     */
    public record Catch(String name, String displayName) {
    }

    private static final Map<String, Catch> BY_MESSAGE = new HashMap<>();

    static {
        // ---- PARK
        put("Pitch darkness reveals a Night Squid.", "Night Squid", "§f", false);

        // ---- CHUMCAP
        put("Your Chumcap Bucket trembles, it's an Agarimoo.", "Agarimoo", "§9", false);

        // ---- CARROT
        put("Is this even a fish? It's the Carrot King!", "Carrot King", "§9", true);

        // ---- WATER
        put("A Squid appeared.", "Squid", "§f", false);
        put("You caught a Sea Walker.", "Sea Walker", "§f", false);
        put("You stumbled upon a Sea Guardian.", "Sea Guardian", "§f", false);
        put("You reeled in a Sea Archer.", "Sea Archer", "§a", false);
        put("The Rider of the Deep has emerged.", "Rider of the Deep", "§a", false);
        put("It looks like you've disrupted the Sea Witch's brewing session. Watch out, she's furious!",
                "Sea Witch", "§a", false);
        put("Huh? A Catfish!", "Catfish", "§9", false);
        put("Gross! A Sea Leech!", "Sea Leech", "§9", false);
        put("You've discovered a Guardian Defender of the sea.", "Guardian Defender", "§5", false);
        put("You have awoken the Deep Sea Protector, prepare for a battle!", "Deep Sea Protector", "§5", false);
        put("The Water Hydra has come to test your strength.", "Water Hydra", "§6", true);

        // ---- WINTER_ISLAND
        put("Frozen Steve fell into the pond long ago, never to resurface...until now!",
                "Frozen Steve", "§f", false);
        put("It's a snowman! He looks harmless.", "Frosty", "§f", false);
        put("The Grinch stole Jerry's Gifts...get them back!", "Grinch", "§a", false);
        put("You found a forgotten Nutcracker laying beneath the ice.", "Nutcracker", "§5", false);
        put("What is this creature!?", "Yeti", "§6", true);
        put("A Reindrake forms from the depths.", "Reindrake", "§d", true);

        // ---- SPOOKY
        put("Phew! It's only a Scarecrow.", "Scarecrow", "§f", false);
        put("You hear trotting from beneath the waves, you caught a Nightmare.", "Nightmare", "§9", false);
        put("It must be a full moon, a Werewolf appears.", "Werewolf", "§5", false);
        put("The spirit of a long lost Phantom Fisher has come to haunt you.", "Phantom Fisher", "§6", true);
        put("This can't be! The manifestation of death himself!", "Grim Reaper", "§6", true);
        put("Watch out! It's Jumpin' Jack.", "Jumpin' Jack", "§f", false);

        // ---- SHARK
        put("A tiny fin emerges from the water, you've caught a Nurse Shark.", "Nurse Shark", "§f", false);
        put("You spot a fin as blue as the water it came from, it's a Blue Shark.", "Blue Shark", "§a", false);
        put("A striped beast bounds from the depths, the wild Tiger Shark!", "Tiger Shark", "§5", false);
        put("Hide no longer, a Great White Shark has tracked your scent and thirsts for your blood!",
                "Great White Shark", "§6", true);

        // ---- OASIS
        put("An Oasis Sheep appears from the water.", "Oasis Sheep", "§a", false);
        put("An Oasis Rabbit appears from the water.", "Oasis Rabbit", "§a", false);

        // ---- ABANDONED_QUARRY
        put("A leech of the mines surfaces... you've caught a Mithril Grubber.",
                "Small Mithril Grubber", "§a", false);
        put("A leech of the mines surfaces... you've caught a Medium Mithril Grubber.",
                "Medium Mithril Grubber", "§a", false);
        put("A leech of the mines surfaces... you've caught a Large Mithril Grubber.",
                "Large Mithril Grubber", "§a", false);
        put("A leech of the mines surfaces... you've caught a Bloated Mithril Grubber.",
                "Bloated Mithril Grubber", "§a", false);

        // ---- MAGMA_FIELDS
        put("A Lava Blaze has surfaced from the depths!", "Lava Blaze", "§9", false);
        put("A Lava Pigman arose from the depths!", "Lava Pigman", "§9", false);

        // ---- LAVA_PRECURSOR
        put("A Flaming Worm surfaces from the depths!", "Flaming Worm", "§9", false);

        // ---- GOBLIN_BURROWS
        put("A Water Worm surfaces!", "Water Worm", "§9", false);
        put("A Poisoned Water Worm surfaces!", "Poisoned Water Worm", "§9", false);

        // ---- WATER_CRYSTAL_HOLLOWS
        put("An Abyssal Miner breaks out of the water!", "Abyssal Miner", "§6", true);

        // ---- LAVA_CRIMSON_ISLE
        put("You hear a faint Moo from the lava... A Moogma appears.", "Moogma", "§9", false);
        put("From beneath the lava appears a Magma Slug.", "Magma Slug", "§9", false);
        put("You feel the heat radiating as a Pyroclastic Worm surfaces.", "Pyroclastic Worm", "§9", false);
        put("A Lava Flame flies out from beneath the lava.", "Lava Flame", "§9", false);
        put("A Fire Eel slithers out from the depths.", "Fire Eel", "§9", false);
        put("A small but fearsome Lava Leech emerges.", "Lava Leech", "§9", false);
        put("Taurus and his steed emerge.", "Taurus", "§9", false);
        put("You hear a massive rumble as Thunder emerges.", "Thunder", "§d", true);
        put("You have angered a legendary creature... Lord Jawbus has arrived.", "Lord Jawbus", "§d", true);

        // ---- PLHLEGBLAST
        put("WOAH! A Plhlegblast appeared.", "Plhlegblast", "§d", true);

        // ---- BACKWATER_BAYOU
        put("The Trash Gobbler is hungry for you!", "Trash Gobbler", "§f", false);
        put("The desolate wail of a Banshee breaks the silence.", "Banshee", "§9", false);
        put("A long snout breaks the surface of the water. It's an Alligator!", "Alligator", "§6", true);
        put("A Dumpster Diver has emerged from the swamp!", "Dumpster Diver", "§a", false);
        put("A swampy mass of slime emerges, the Bayou Sludge!", "Bayou Sludge", "§5", false);
        put("A massive Titanoboa surfaces. Its body stretches as far as the eye can see.", "Titanoboa", "§d", true);

        // ---- LAVA_HOTSPOT
        put("The sky darkens and the air thickens. The end times are upon us: Ragnarok is here.",
                "Ragnarok", "§d", true);
        put("You feel a burning sensation as you reel in a Volcanic Snail!", "Volcanic Snail", "§a", false);
        put("Trouble's brewing, it's a Fireproof Witch!", "Fireproof Witch", "§9", false);
        put("Smells of burning. Must be a Fried Chicken.", "Fried Chicken", "§f", false);
        put("A Magma Pillar rises from the lava.", "Magma Pillar", "§5", true);
        put("A Fiery Scuttler inconspicuously waddles up to you, friends in tow.", "Fiery Scuttler", "§6", true);

        // ---- WATER_HOTSPOT
        put("The water bubbles and froths. A massive form emerges- you have disturbed the Wiki Tiki! You shall pay the price.",
                "Wiki Tiki", "§d", true);
        put("A garish set of tentacles arise. It's a Blue Ringed Octopus!", "Blue Ringed Octopus", "§6", true);
        put("A Snapping Turtle is coming your way, and it's ANGRY!", "Snapping Turtle", "§9", false);
        put("Is it a frog? Is it a man? Well, yes, sorta, IT'S FROG MAN!!!!!!", "Frog Man", "§f", false);
        put("You get an inkling that you've caught... an Inkling!", "Inkling", "§a", false);
        put("A majestic creature rises from the water. It's a Manta Ray.", "Manta Ray", "§5", false);

        // ---- MOONGLADE_MARSH
        put("Look! A Wetwing emerges!", "Wetwing", "§a", false);
        put("You've hooked an Ent, as ancient as the forest itself.", "Ent", "§5", false);
        put("A gang of Liltads!", "Tadgang", "§9", false);
        put("You've hooked a Bogged!", "Bogged", "§f", false);
        put("You caught a Stridersurfer.", "Stridersurfer", "§9", false);
        put("The Loch Emperor arises from the depths.", "The Loch Emperor", "§6", true);
        put("You've caused a disturbance in the loch. Could it be... Nessie?", "Nessie", "§d", true);

        // ---- LOTUS_ATOLL
        put("An inquisitive Atoll Croaker takes the bait!", "Atoll Croaker", "§f", false);
        put("A Lotus Guardian emerges, ready to protect the Atoll.", "Lotus Guardian", "§a", false);
        put("What even is that?! A... gorF?", "gorF", "§9", false);
        put("A Drowned Captain takes hold of your bobber!", "Drowned Captain", "§5", false);
        put("A Puddle Jumper is preparing for liftoff—cast your rod into it and hold on tight!",
                "Puddle Jumper", "§6", true);
        put("Bow down before the Frog Prince... or pay the hefty price!", "Frog Prince", "§d", true);

        // ---- TORRHUS_CANYON
        put("A Haggard stumbles to the shore, ready for a fight!", "Haggard", "§f", false);
        put("A Brineling interrupts you with a stream of bubbles!", "Brineling", "§a", false);
        put("A Sprawl emerges from the blue, and it's looking for you!", "Sprawl", "§9", false);
        put("The laughter of a Torrid echoes through the air.", "Torrid", "§5", false);
        put("Something zips through the air - it's a Silkbreeze!", "Silkbreeze", "§6", true);
        put("A Giant Isopod was dredged up from the depths!", "Giant Isopod", "§d", true);
    }

    private SeaCreatureCatches() {
    }

    private static void put(String message, String name, String colorCode, boolean rare) {
        BY_MESSAGE.put(message, new Catch(name, colorCode + (rare ? "§l" : "") + name));
    }

    /** 色コードを落とした文言から引く。Sea Creature の釣り上げでなければ null */
    public static Catch byMessage(String strippedMessage) {
        return strippedMessage == null ? null : BY_MESSAGE.get(strippedMessage.trim());
    }
}
