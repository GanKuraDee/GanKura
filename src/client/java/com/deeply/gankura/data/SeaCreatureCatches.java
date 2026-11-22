package com.deeply.gankura.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.HashMap;
import java.util.Map;

/**
 * Sea Creature を釣り上げたときにチャットへ出る文言と、その種類。
 *
 * SkyHanni が使っている SkyHanni-REPO の constants/SeaCreatures.json から起こしたもの。
 * 色コードを落とした文言をそのまま鍵にして引く。
 * 部分一致にすると誰かの発言や別の文言に巻き込まれるため、丸ごと一致で見る。
 */
public final class SeaCreatureCatches {

    /**
     * 釣り上げた Sea Creature。
     *
     * name は色なしの名前、rarityColor はレア度の色、rare は太字にするか、
     * lava はマグマ釣り(Magmatic)か水釣り(Aquatic)かを表す
     */
    public record Catch(String name, ChatFormatting rarityColor, boolean rare, boolean lava) {
        public MutableComponent formattedName() {
            Style style = Style.EMPTY.withColor(rarityColor).withBold(rare);
            return Component.literal(name).withStyle(style);
        }
    }

    private static final Map<String, Catch> BY_MESSAGE = new HashMap<>();

    static {
        // ---- PARK
        putWater("Pitch darkness reveals a Night Squid.", "Night Squid", ChatFormatting.WHITE, false);

        // ---- CHUMCAP
        putWater("Your Chumcap Bucket trembles, it's an Agarimoo.", "Agarimoo", ChatFormatting.BLUE, false);

        // ---- CARROT
        putWater("Is this even a fish? It's the Carrot King!", "Carrot King", ChatFormatting.BLUE, true);

        // ---- WATER
        putWater("A Squid appeared.", "Squid", ChatFormatting.WHITE, false);
        putWater("You caught a Sea Walker.", "Sea Walker", ChatFormatting.WHITE, false);
        putWater("You stumbled upon a Sea Guardian.", "Sea Guardian", ChatFormatting.WHITE, false);
        putWater("You reeled in a Sea Archer.", "Sea Archer", ChatFormatting.GREEN, false);
        putWater("The Rider of the Deep has emerged.", "Rider of the Deep", ChatFormatting.GREEN, false);
        putWater("It looks like you've disrupted the Sea Witch's brewing session. Watch out, she's furious!",
                "Sea Witch", ChatFormatting.GREEN, false);
        putWater("Huh? A Catfish!", "Catfish", ChatFormatting.BLUE, false);
        putWater("Gross! A Sea Leech!", "Sea Leech", ChatFormatting.BLUE, false);
        putWater("You've discovered a Guardian Defender of the sea.", "Guardian Defender", ChatFormatting.DARK_PURPLE, false);
        putWater("You have awoken the Deep Sea Protector, prepare for a battle!", "Deep Sea Protector", ChatFormatting.DARK_PURPLE, false);
        putWater("The Water Hydra has come to test your strength.", "Water Hydra", ChatFormatting.GOLD, true);

        // ---- WINTER_ISLAND
        putWater("Frozen Steve fell into the pond long ago, never to resurface...until now!",
                "Frozen Steve", ChatFormatting.WHITE, false);
        putWater("It's a snowman! He looks harmless.", "Frosty", ChatFormatting.WHITE, false);
        putWater("The Grinch stole Jerry's Gifts...get them back!", "Grinch", ChatFormatting.GREEN, false);
        putWater("You found a forgotten Nutcracker laying beneath the ice.", "Nutcracker", ChatFormatting.DARK_PURPLE, false);
        putWater("What is this creature!?", "Yeti", ChatFormatting.GOLD, true);
        putWater("A Reindrake forms from the depths.", "Reindrake", ChatFormatting.LIGHT_PURPLE, true);

        // ---- SPOOKY
        putWater("Phew! It's only a Scarecrow.", "Scarecrow", ChatFormatting.WHITE, false);
        putWater("You hear trotting from beneath the waves, you caught a Nightmare.", "Nightmare", ChatFormatting.BLUE, false);
        putWater("It must be a full moon, a Werewolf appears.", "Werewolf", ChatFormatting.DARK_PURPLE, false);
        putWater("The spirit of a long lost Phantom Fisher has come to haunt you.", "Phantom Fisher", ChatFormatting.GOLD, true);
        putWater("This can't be! The manifestation of death himself!", "Grim Reaper", ChatFormatting.GOLD, true);
        putWater("Watch out! It's Jumpin' Jack.", "Jumpin' Jack", ChatFormatting.WHITE, false);

        // ---- SHARK
        putWater("A tiny fin emerges from the water, you've caught a Nurse Shark.", "Nurse Shark", ChatFormatting.WHITE, false);
        putWater("You spot a fin as blue as the water it came from, it's a Blue Shark.", "Blue Shark", ChatFormatting.GREEN, false);
        putWater("A striped beast bounds from the depths, the wild Tiger Shark!", "Tiger Shark", ChatFormatting.DARK_PURPLE, false);
        putWater("Hide no longer, a Great White Shark has tracked your scent and thirsts for your blood!",
                "Great White Shark", ChatFormatting.GOLD, true);

        // ---- OASIS
        putWater("An Oasis Sheep appears from the water.", "Oasis Sheep", ChatFormatting.GREEN, false);
        putWater("An Oasis Rabbit appears from the water.", "Oasis Rabbit", ChatFormatting.GREEN, false);

        // ---- ABANDONED_QUARRY
        putWater("A leech of the mines surfaces... you've caught a Mithril Grubber.",
                "Small Mithril Grubber", ChatFormatting.GREEN, false);
        putWater("A leech of the mines surfaces... you've caught a Medium Mithril Grubber.",
                "Medium Mithril Grubber", ChatFormatting.GREEN, false);
        putWater("A leech of the mines surfaces... you've caught a Large Mithril Grubber.",
                "Large Mithril Grubber", ChatFormatting.GREEN, false);
        putWater("A leech of the mines surfaces... you've caught a Bloated Mithril Grubber.",
                "Bloated Mithril Grubber", ChatFormatting.GREEN, false);

        // ---- MAGMA_FIELDS
        putLava("A Lava Blaze has surfaced from the depths!", "Lava Blaze", ChatFormatting.BLUE, false);
        putLava("A Lava Pigman arose from the depths!", "Lava Pigman", ChatFormatting.BLUE, false);

        // ---- LAVA_PRECURSOR
        putLava("A Flaming Worm surfaces from the depths!", "Flaming Worm", ChatFormatting.BLUE, false);

        // ---- GOBLIN_BURROWS
        putWater("A Water Worm surfaces!", "Water Worm", ChatFormatting.BLUE, false);
        putWater("A Poisoned Water Worm surfaces!", "Poisoned Water Worm", ChatFormatting.BLUE, false);

        // ---- WATER_CRYSTAL_HOLLOWS
        putWater("An Abyssal Miner breaks out of the water!", "Abyssal Miner", ChatFormatting.GOLD, true);

        // ---- LAVA_CRIMSON_ISLE
        putLava("You hear a faint Moo from the lava... A Moogma appears.", "Moogma", ChatFormatting.BLUE, false);
        putLava("From beneath the lava appears a Magma Slug.", "Magma Slug", ChatFormatting.BLUE, false);
        putLava("You feel the heat radiating as a Pyroclastic Worm surfaces.", "Pyroclastic Worm", ChatFormatting.BLUE, false);
        putLava("A Lava Flame flies out from beneath the lava.", "Lava Flame", ChatFormatting.BLUE, false);
        putLava("A Fire Eel slithers out from the depths.", "Fire Eel", ChatFormatting.BLUE, false);
        putLava("A small but fearsome Lava Leech emerges.", "Lava Leech", ChatFormatting.BLUE, false);
        putLava("Taurus and his steed emerge.", "Taurus", ChatFormatting.BLUE, false);
        putLava("You hear a massive rumble as Thunder emerges.", "Thunder", ChatFormatting.LIGHT_PURPLE, true);
        putLava("You have angered a legendary creature... Lord Jawbus has arrived.", "Lord Jawbus", ChatFormatting.LIGHT_PURPLE, true);

        // ---- PLHLEGBLAST
        putLava("WOAH! A Plhlegblast appeared.", "Plhlegblast", ChatFormatting.LIGHT_PURPLE, true);

        // ---- BACKWATER_BAYOU
        putWater("The Trash Gobbler is hungry for you!", "Trash Gobbler", ChatFormatting.WHITE, false);
        putWater("The desolate wail of a Banshee breaks the silence.", "Banshee", ChatFormatting.BLUE, false);
        putWater("A long snout breaks the surface of the water. It's an Alligator!", "Alligator", ChatFormatting.GOLD, true);
        putWater("A Dumpster Diver has emerged from the swamp!", "Dumpster Diver", ChatFormatting.GREEN, false);
        putWater("A swampy mass of slime emerges, the Bayou Sludge!", "Bayou Sludge", ChatFormatting.DARK_PURPLE, false);
        putWater("A massive Titanoboa surfaces. Its body stretches as far as the eye can see.", "Titanoboa", ChatFormatting.LIGHT_PURPLE, true);

        // ---- LAVA_HOTSPOT
        putLava("The sky darkens and the air thickens. The end times are upon us: Ragnarok is here.",
                "Ragnarok", ChatFormatting.LIGHT_PURPLE, true);
        putLava("You feel a burning sensation as you reel in a Volcanic Snail!", "Volcanic Snail", ChatFormatting.GREEN, false);
        putLava("Trouble's brewing, it's a Fireproof Witch!", "Fireproof Witch", ChatFormatting.BLUE, false);
        putLava("Smells of burning. Must be a Fried Chicken.", "Fried Chicken", ChatFormatting.WHITE, false);
        putLava("A Magma Pillar rises from the lava.", "Magma Pillar", ChatFormatting.DARK_PURPLE, true);
        putLava("A Fiery Scuttler inconspicuously waddles up to you, friends in tow.", "Fiery Scuttler", ChatFormatting.GOLD, true);

        // ---- WATER_HOTSPOT
        putWater("The water bubbles and froths. A massive form emerges- you have disturbed the Wiki Tiki! You shall pay the price.",
                "Wiki Tiki", ChatFormatting.LIGHT_PURPLE, true);
        putWater("A garish set of tentacles arise. It's a Blue Ringed Octopus!", "Blue Ringed Octopus", ChatFormatting.GOLD, true);
        putWater("A Snapping Turtle is coming your way, and it's ANGRY!", "Snapping Turtle", ChatFormatting.BLUE, false);
        putWater("Is it a frog? Is it a man? Well, yes, sorta, IT'S FROG MAN!!!!!!", "Frog Man", ChatFormatting.WHITE, false);
        putWater("You get an inkling that you've caught... an Inkling!", "Inkling", ChatFormatting.GREEN, false);
        putWater("A majestic creature rises from the water. It's a Manta Ray.", "Manta Ray", ChatFormatting.DARK_PURPLE, false);

        // ---- MOONGLADE_MARSH
        putWater("Look! A Wetwing emerges!", "Wetwing", ChatFormatting.GREEN, false);
        putWater("You've hooked an Ent, as ancient as the forest itself.", "Ent", ChatFormatting.DARK_PURPLE, false);
        putWater("A gang of Liltads!", "Tadgang", ChatFormatting.BLUE, false);
        putWater("You've hooked a Bogged!", "Bogged", ChatFormatting.WHITE, false);
        putWater("You caught a Stridersurfer.", "Stridersurfer", ChatFormatting.BLUE, false);
        putWater("The Loch Emperor arises from the depths.", "The Loch Emperor", ChatFormatting.GOLD, true);
        putWater("You've caused a disturbance in the loch. Could it be... Nessie?", "Nessie", ChatFormatting.LIGHT_PURPLE, true);

        // ---- LOTUS_ATOLL
        putWater("An inquisitive Atoll Croaker takes the bait!", "Atoll Croaker", ChatFormatting.WHITE, false);
        putWater("A Lotus Guardian emerges, ready to protect the Atoll.", "Lotus Guardian", ChatFormatting.GREEN, false);
        putWater("What even is that?! A... gorF?", "gorF", ChatFormatting.BLUE, false);
        putWater("A Drowned Captain takes hold of your bobber!", "Drowned Captain", ChatFormatting.DARK_PURPLE, false);
        putWater("A Puddle Jumper is preparing for liftoff—cast your rod into it and hold on tight!",
                "Puddle Jumper", ChatFormatting.GOLD, true);
        putWater("Bow down before the Frog Prince... or pay the hefty price!", "Frog Prince", ChatFormatting.LIGHT_PURPLE, true);

        // ---- TORRHUS_CANYON
        putWater("A Haggard stumbles to the shore, ready for a fight!", "Haggard", ChatFormatting.WHITE, false);
        putWater("A Brineling interrupts you with a stream of bubbles!", "Brineling", ChatFormatting.GREEN, false);
        putWater("A Sprawl emerges from the blue, and it's looking for you!", "Sprawl", ChatFormatting.BLUE, false);
        putWater("The laughter of a Torrid echoes through the air.", "Torrid", ChatFormatting.DARK_PURPLE, false);
        putWater("Something zips through the air - it's a Silkbreeze!", "Silkbreeze", ChatFormatting.GOLD, true);
        putWater("A Giant Isopod was dredged up from the depths!", "Giant Isopod", ChatFormatting.LIGHT_PURPLE, true);
    }

    private SeaCreatureCatches() {
    }

    private static void putWater(String message, String name, ChatFormatting rarityColor, boolean rare) {
        BY_MESSAGE.put(message, new Catch(name, rarityColor, rare, false));
    }

    private static void putLava(String message, String name, ChatFormatting rarityColor, boolean rare) {
        BY_MESSAGE.put(message, new Catch(name, rarityColor, rare, true));
    }

    /** 色コードを落とした文言から引く。Sea Creature の釣り上げでなければ null */
    public static Catch byMessage(String strippedMessage) {
        return strippedMessage == null ? null : BY_MESSAGE.get(strippedMessage.trim());
    }
}
