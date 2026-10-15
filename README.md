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
The sections below follow the order and the names of the settings screen (`/gankura`).

<details>
<summary>Edit HUD Locations</summary>

  - Move and scale every HUD this mod draws (also `/gankura hud`).
</details>

<details>
<summary>Combat</summary>

  - Hide Damage Splash
  - Hide Fire Overlay
  - Cocoon Catch Title
  - Ragnarock - Cast Title, Strength in Chat, Cancel Title, Countdown HUD
  - Arrow Poison Indicator
  - Armor Stack HUD
  - Ferocity HUD
  - Quiver HUD
  - Low Quiver Alert - Enable, Threshold
  - Soulflow HUD
  - Low Soulflow Alert - Enable, Threshold
  - **The End**
    - End Stone Protector
      - Status HUD
      - Rare Drop Settings - Loot Tracker HUD, Notification, Tracked Drops
![Rare Drop Notification](https://cdn.modrinth.com/data/cached_images/083fb73c0c606473ca634b7548aca070825fc15f.png)
      - World Location Display - Show Text, Show Beacon Beam, Show Tracer
![End Stone Protector World Location Display](https://cdn.modrinth.com/data/cached_images/f0cd9155cc45df09483a68779fc5cbeaa1ef9054.png)
      - Stage 4 Alert / Stage 5 Alert - Show Title, Play Sound
![End Stone Protector Stage Announce](https://cdn.modrinth.com/data/cached_images/a7793cf8866fd46f7ff8347aba825bc6922b3bbc.png)
      - Chat Settings - Stage 4 Duration Message, DPS Message, Loot Quality Message, Day 30+ Alert Message
    - Dragon
      - Status HUD
      - Rare Drop Settings - Loot Tracker HUD, Notification, Tracked Drops
      - Spawn Alert Title - Enable, Alert Dragons
      - Chat Settings - DPS Message, Loot Quality Message
  - **Spider's Den**
    - Broodmother
      - Status HUD
      - Stage 4 Alert / Stage 5 Alert - Show Title, Play Sound
      - Chat Settings - Stage 4 Duration Message
    - Arachne
      - Status HUD
      - World Location Display - Show Text
  - **Crimson Isle**
    - Status HUD
    - Rare Drop Settings - Loot Tracker HUD, Notification, Tracked Drops
    - World Location Display - Show Text
    - Magma Boss - Stage Status Title
</details>

<details>
<summary>Farming</summary>

  - DNA Analyzer - Solver, Middle Click, Block Close Button, Hide Tooltips
  - **Garden**
    - Visitor - Status HUD, Required Items Panel, Highlight Accept Offer, Arrival Title, Queue Full Title
    - Jacob's Contest - Highlight Rewards
    - Pest - Status HUD, Spawn Title, Max Pests Title, Spawn Soon Title
      - Vacuum - Waypoint, Tracer, Hide Particles
    - Squeaky Mousemat - Lock View, Release on Teleport, Release Key
</details>

<details>
<summary>Foraging</summary>

  - Title Settings - Tree Felled Title, Mob From Tree Title
  - Torrhus Canyon - Tiki Spawn Waypoints, Beeheemoth Spawn Title
  - Critter Safari
    - Captured Critters HUD
    - Floor Drops
    - Honeybug - Bee Nest Waypoints
    - Scrappy - Fish Highlight
    - Rockmite - Mound Highlight
    - Wumpa - Spawn Title, Capsule Usage Message, Re-enter Waypoint
    - Doomspiral - Capsule Usage Message
    - Macaw - Spawn Title
</details>

<details>
<summary>Fishing</summary>

  - HUD Settings - Bite Countdown HUD, Bait HUD
  - Chat Settings - Shorten Catch Message
  - Low Bait Alert - Enable, Threshold
  - Cast Timer - Enable, Start on Liquid Touch
  - Hotspot
    - Radar Guess - Reads the Hotspot Radar trail and marks where the hotspot should be.
    - Gone Title
    - Circle - Draws the hotspot edge as a circle, coloured by its perk.
    - Found Alert - Title, sound, temporary waypoint and tracer when a hotspot with the perks
      you picked shows up, plus a button to share its coordinates.
  - Lava as Water - Draws lava with the water texture and clears the orange view inside it,
    on Crimson Isle or everywhere.
  - Golden Fish - Highlight, Tracer, Throw Rod Warning, spawn window timer
</details>

<details>
<summary>Mining</summary>

  - Commissions - Highlight Completed, Gemstone Waypoints
  - Mineshaft
    - Corpse - Waypoints, Tracer, Vanguard Title, Profit Message
  - Wishing Compass - Solver, Waypoint
</details>

<details>
<summary>General HUD</summary>

  - Armor HUD
  - Equipment HUD
  - Yaw and Pitch HUD
  - Pet HUD
  - TPS HUD
  - Day HUD
</details>

<details>
<summary>Mob Visuals</summary>
  
  - Highlight, Tracer and Nameplate on the mobs picked for each area.
![Mob Visuals 1](https://cdn.modrinth.com/data/cached_images/cedfcc59352699485521d31dd54cea2fdc8c0bf5.png)
![Mob Visuals 2](https://cdn.modrinth.com/data/cached_images/492e5e063322ab0882b7efb41fc43e0836a90fdc.png)
  - The End, Spider's Den, Crimson Isle, Crystal Hollows, Moonglade Marsh, Torrhus Canyon,
    Lotus Atoll, Glacite Mineshaft, Garden, Critter Safari, Sea Creatures
  - Critter Safari - Hide Captured Critters
  - Sea Creatures - Spawn Title, Spawn Sound
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

- **Button Editor** (`/gankura buttons`, or the button in the settings screen)
  - Click a slot around the inventory to edit it, click it again to close the panel.
  - **Command** -> The command the button runs. A slot with no command stays hidden in game.
  - **Background** -> One of five frame styles.
  - **Icon Type** -> Item, head or built-in icon.
  - **Icon Selector** -> Type to filter, then click an icon.
  - **Presets** -> The list on the right. Click a name to load it, right click to delete it. `Empty` is built in and restores the default layout.
  - **Save Preset** -> Saves the current buttons to the preset list under the name typed above the button (up to 12).
- **Options**
  - Hide in Dungeon Menus -> Keeps the buttons out of dungeon puzzle menus.
  - Click Type -> Run the command on press or on release.
  - Tooltip Delay -> How long the cursor rests on a button before its command is shown.
![Inventory Buttons](https://cdn.modrinth.com/data/cached_images/b653085fbbbe946318af5927c82b4a4c727da7bd.png)
</details>

<details>
<summary>Viewmodel</summary>

  - Size, X, Y - The size and position of the item in your hand.
</details>

<details>
<summary>Interface</summary>

  - Highlight Opacity
  - Search Input Screen - Replaces the Bazaar and Auction House sign editor with a text box.
  - Inventory - Hide Potion Effects, Hide Background Dim, Hide Recipe Book
  - Tab List - Hide Ads, Hide Ping Icons, Hide Extra Heads, Compact Columns, Fit Column Width, Shrink To Fit
  - Tooltips - Scrollable Tooltips, Invert Tooltip Scroll, Tooltip From Top
    - Enchant - Replace Roman Numerals, Max Enchant Chroma, Book Enchant Gold, Skip Ultimate Enchants
    - Item Price - Lowest BIN, Bazaar, Craft Cost, Stack Price, Short Numbers
  - Auction/Bazaar - Order Helper, Manage Auctions, Undercut Highlight
  - Attribute Menu - Replace Roman Numerals, Highlight Progress, Show Tier, Hide Maxed Tier, Shard Cost Panel
  - Bestiary Menu - Replace Roman Numerals, Highlight Progress, Show Tier, Hide Maxed Tier
  - Hunting Box - Show Owned, Value Panel
  - Heart of the Forest / Mountain - Highlight Status, Show Level
  - Enchanted Book - Show Name, Show Tier
  - Pets - Highlight Active Pet, Show Level, Hide Maxed Level
  - Personal Compactor - Show Status
  - NPC Sell Protection - Minimum Rarity, Protect Recombobulated, Triple-Click to Sell
  - Keep Cursor Position
</details>

<details>
<summary>Chat Filter</summary>

  - Hide Profile Name
  - Hide Profile ID
  - Hide Stash Reminder
  - Hide Blank Lines
</details>

<details>
<summary>Keybinds</summary>

  - Open Menu Keybind - Loadouts, Wardrobe, Equipment
  - Loadouts Keybind - Slot 1 to 12
  - Armor Set Keybind - Slot 1 to 9
  - Equipment Set Keybind - Slot 1 to 9
</details>

<details>
<summary>Misc</summary>

  - Etherwarp - Highlight Target, Only While Sneaking, Show Reason
  - Server Reboot Alert
  - Warp Cooldown Queue
  - Ability Menu Fix
  - Hide Block Break Particles
  - Ignore Armor Stand Clicks
</details>

# Credits
- [NotEnoughUpdates](https://github.com/NotEnoughUpdates/NotEnoughUpdates) (LGPL-3.0-or-later) - the Inventory Buttons feature, its button textures, icons and presets.
- [SkyHanni-REPO](https://github.com/hannibal002/SkyHanni-REPO) (MIT, Copyright (c) 2022 hannibal2) - the enchantment level limits used by Max Enchant Chroma and Book Enchant Gold.
