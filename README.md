# GanKura
GanKura is a Hypixel Skyblock Mod focused on Area Mini-bosses. 

- **Commands**
  - `/gankura` - Open the main settings screen.
  - `/gankura hud` - Open the HUD movement and scaling settings screen.
  - `/gankura buttons` - Open the Inventory Buttons editor screen.
  - `/gankura waypoint <arg> (/gkw)` - Open the Custom Waypoints settings screen.
    - `toggle` - Toggle the rendering of Waypoints.
    - `add` - Add a Waypoint to your current location.
    - `add <name>` - Add a named Waypoint to your current location.
    - `list` - Display a list of Waypoints in the current area.

# Supported
- **The End**
  - End Stone Protector
  - Dragon
- **Spider's Den**
  - Broodmother
  - Arachne
- **Crimson Isle**
  - Bladesoul
  - Barbarian Duke X
  - Mage Outlaw
  - Magma Boss
  - Ashfang
- **Foraging**
  - All Moonglade Marsh Mobs (Excluding Sea Creatures)
  - All Torrhus Canyon Mobs (Excluding Sea Creatures)
  - All Critter Safari Mobs
- **Fishing**
  - All Rare Sea Creatures
  - All Lotus Atoll Critters

# Features
<details>
<summary>Bosses</summary>

  - Stage Announce
![End Stone Protector Stage Announce](https://cdn.modrinth.com/data/cached_images/a7793cf8866fd46f7ff8347aba825bc6922b3bbc.png)
  - World Location Display
![End Stone Protector World Location Display](https://cdn.modrinth.com/data/cached_images/f0cd9155cc45df09483a68779fc5cbeaa1ef9054.png)
  - Rare Drop Notification
![Rare Drop Notification](https://cdn.modrinth.com/data/cached_images/083fb73c0c606473ca634b7548aca070825fc15f.png)
  - Status HUD
  - Loot Tracker HUD
  - DPS Calculator
  - Loot Quality Calculator

</details>

<details>
<summary>Misc</summary>
  
  - Armor HUD
  - Equipment HUD
  - Yaw and Pitch HUD
  - Active Pet HUD
  - Day HUD
  - TPS HUD
  - Armor Stack HUD
  - Ferocity HUD
  - Quiver HUD
  - Hide Damage Splash
  - Hide Fire Overlay
  - Tab List Cleanup
  - Inventory Cleanup
  - Scrollable Tooltips
  - Invert Tooltip Scroll
  - Tooltip From Top
  - Replace Roman Numerals
  - Max Enchant Chroma
  - Book Enchant Gold
  - Skip Ultimate Enchants
  - Arrow Poison Indicator
  - Server Reboot Alert
  - Low Quiver Alert
  - Warp Cooldown Queue
  - Keep Cursor Position
  - Held Item Size
  - Loadouts Menu Keybind
  - Armor Menu Keybind
  - Equipment Menu Keybind
  - Tree Felled Title
  - Mob From Tree Title
  - Beeheemoth Spawn Title
  - Cocoon Catch Title
  - Bestiary Menu - Replace Roman Numerals, MAX highlight and per-slot tier numbers.
  - Attribute Menu - Replace Roman Numerals, MAX highlight, per-slot tier numbers and a shard cost list.
  - Pets - Per-slot level numbers and a highlight on the pet you have out.
  - Item Price - Writes the Auction House lowest BIN, the Bazaar price and the craft cost
    under item tooltips.
  - Manage Auctions - Marks your own auctions: green when sold, red when expired, yellow when undercut.
  - Bazaar Order Helper - Marks your Bazaar orders yellow when you are outbid, green when filled.
  - Search Input Screen - Replaces the Bazaar and Auction House sign editor with a text box.
  - Chat Filter - Hides the profile lines, the stash reminder and blank chat lines.
</details>

<details>
<summary>Fishing</summary>

  - Shorten Catch Message
  - Bite Countdown HUD
  - Bait HUD
  - Low Bait Alert
  - Cast Timer
  - Hotspot
    - Radar Guess - Reads the Hotspot Radar trail and marks where the hotspot should be.
    - Gone Title
    - Circle - Draws the hotspot edge as a circle, coloured by its perk.
    - Found Alert - Title, sound, temporary waypoint and tracer when a hotspot with the perks
      you picked shows up, plus a button to share its coordinates.
  - Golden Fish
    - Spawn window timer, highlight, tracer and throw rod warning.
  - Lava as Water - Draws lava with the water texture and clears the orange view inside it,
    on Crimson Isle or everywhere.
</details>

<details>
<summary>Mob Visuals</summary>
  
  - Display highlights, tracers, and nameplates on selected mobs.
![Mob Visuals 1](https://cdn.modrinth.com/data/cached_images/cedfcc59352699485521d31dd54cea2fdc8c0bf5.png)
![Mob Visuals 2](https://cdn.modrinth.com/data/cached_images/492e5e063322ab0882b7efb41fc43e0836a90fdc.png)
</details>

<details>
<summary>Custom Waypoints</summary>
  
- **Add at my position / Add empty:**
  - Add Waypoint to your current location / coordinates x:0,y:0,z:0
- **Area:**
  - '<, >' -> Switch between areas where you have created Waypoints.
- **Group:** 
  - '+' -> Add a Group 
  - '<, >' -> Switch between the groups you have created
  - '✎' -> Rename current group
  - '✖' -> Remove current group
- **Waypoint:**
  - Name -> Name of Waypoint to display in the world
  - X, Y, Z -> Coordinates of Waypoint
  - Color -> Settings the color and fill opacity of the Waypoint
  - Style:
    - Both -> Outline and Fill
    - Outline -> Displays only Outline
    - Fill -> Displays only Fill
  - '⇄' -> Move selected Waypoint to another group
  - '✖' -> Remove selected Waypoint
![Custom Waypoints 1](https://cdn.modrinth.com/data/cached_images/8b39be6a666a8a36f4844bc7f39bdd618cea4f0f.png)
![Custom Waypoints 2](https://cdn.modrinth.com/data/cached_images/c9685eeb75c9bd7a20cb900d570b24ac5010febe.png)
</details>

<details>
<summary>Inventory Buttons</summary>

Buttons placed around inventory menus that run a command when clicked. Ported from [NotEnoughUpdates](https://github.com/NotEnoughUpdates/NotEnoughUpdates).

- **Editor** (`/gankura buttons`, or the button in the settings screen)
  - Click a slot around the inventory to edit it, click it again to close the panel.
  - **Command** -> The command the button runs. A slot with no command stays hidden in game.
  - **Background** -> One of five frame styles.
  - **Icon Type** -> Item, head or built-in icon.
  - **Icon Selector** -> Type to filter, then click an icon.
  - **Presets** -> The list on the right. Click a name to load it, right click to delete it. `Empty` is built in and restores the default layout.
  - **Save Preset** -> Saves the current buttons to the preset list under the name typed above the button (up to 12).
- **Options**
  - Click Type -> Run the command on press or on release.
  - Tooltip Delay -> How long the cursor rests on a button before its command is shown.
  - Hide in Dungeon Menus -> Keeps the buttons out of dungeon puzzle menus.
![Inventory Buttons](https://cdn.modrinth.com/data/cached_images/b653085fbbbe946318af5927c82b4a4c727da7bd.png)
</details>

# Credits
- [NotEnoughUpdates](https://github.com/NotEnoughUpdates/NotEnoughUpdates) (LGPL-3.0-or-later) - the Inventory Buttons feature, its button textures, icons and presets.
- [SkyHanni-REPO](https://github.com/hannibal002/SkyHanni-REPO) (MIT, Copyright (c) 2022 hannibal2) - the enchantment level limits used by Max Enchant Chroma and Book Enchant Gold.
