# SkyBlock Profit Counter (Hypixel SkyBlock, Fabric 26.1.2)

Tracks profit for **farming, mining, foraging, fishing and combat/slayers**, each with its own HUD.
The HUD automatically shows whatever you did in the last minute (or the island you're on).
Every session resets after 15 minutes of no activity and is saved to a history log.

## What counts as activity
| HUD | Starts / stays alive when you... |
|---|---|
| Farming | break crops, kill pests, use a vacuum |
| Mining | mine ores/gemstones in Dwarven Mines, Crystal Hollows, Glacite Mineshafts (one combined HUD) |
| Foraging | chop logs on Galatea / The Park, get Tree Gifts |
| Fishing | cast or reel a fishing rod, kill sea creatures |
| Combat | hit mobs anywhere else, start a slayer quest, kill a slayer boss |

## What's counted as profit
- Items going into your inventory or sacks (anything stackable that has a Bazaar / lowest-BIN price)
- Rare drops (RARE DROP!, PET DROP!, GOOD/GREAT/OUTSTANDING CATCH! ...) that weren't already counted as items
- Attribute shards (they go to the Hunting Box)
- Minus slayer quest costs, if you set `slayerQuestCost` in the config

## Commands
`/profit` shows the HUD you're looking at. `/farmprofit`, `/miningprofit`, `/foragingprofit`, `/fishingprofit`
and `/combatprofit` show that specific activity. All of them print **profit only** (time, profit, profit/h,
shards, rare drops, every item).

| Sub-command | What it does |
|---|---|
| `total` | Lifetime totals (all activities, or just that one) |
| `copy` | Copy "Mining: 12.3M coins in 2h 10m (5.6M/h)" to your clipboard |
| `ignore <item>` / `unignore <item>` | Stop / start counting an item (e.g. `/profit ignore Hay Bale`) |
| `history [count]` | Past sessions (`/profit history` = all, `/miningprofit history` = mining only) |
| `reset` | End that session now (still saved to history) |
| `scale <size>` | HUD size (0.5–3) |
| `icons` | Item icons on/off |
| `edit` | How to move the HUD / hide items |
| `details` | Toggle the HUD between full details and profit only |
| `hud` | HUD on/off |
| `move <x> <y>` | Move the HUD |
| `prices` | Refresh prices |
| `reload` | Reload the config |

## Moving & editing the HUD
Open chat (**T**). While chat is open:
- **Left-click and drag** the HUD to move it.
- **Right-click** an item, shard or rare-drop line to hide it (it stops counting toward profit). Undo with `/profit unignore <item>`.

`/profit scale <0.5-3>` resizes the HUD, `/profit icons` toggles item icons (icons appear for items that have been in your inventory).

## Slayer quest costs
Detected automatically from your purse on the sidebar when a quest starts (handles Aatrox discounts etc.).
If you'd rather use a fixed number, set `slayerQuestCost` in the config (anything above 0 overrides auto-detection).

## New in 5.2
- **`/talismans [count]`**: the next accessories to get, ranked by **coins per Magical Power**, using the cheaper of the
  Auction House (lowest BIN) and crafting. Upgrade chains are understood (Talisman → Ring → Artifact): if you own a lower
  tier, only the extra MP counts and the part you own is free in the craft cost. One entry per chain (the best value tier).
  Click an entry to search the AH or open its recipe. What you own is read when you open your **Accessory Bag** (open
  every page once, and again after buying). Settings → Items & areas: how many to list, include crafting, max price.
  Accessories only sold by NPCs or not tradeable have no price here and are skipped.

## New in 5.1
More dungeon puzzle solvers. Answers appear as **particles in the world** that only you see, plus a line on the HUD:
- **Ice Fill**: a green path over every ice tile from where you stand (recalculated as you walk).
- **Creeper Beams**: each lantern pair to connect gets its own particle colour and a line through the creeper.
- **Teleport Maze**: after each teleport, the pad most in the direction you face (toward the exit) is marked; used pads are skipped.
- **Tic Tac Toe**: reads the X / O maps on the wall and marks the best button (unbeatable play).
- **Quiz (Ouro)**: tells you the right answer letter, using the maintained Skytils answer list (and works out "What SkyBlock year is it?" itself).
- **Melody terminal** (F7): the button turns green when it's time to click.
Each one can be turned off in Settings → Dungeons. Still not included: Boulder and Water Board (see below).

## New in 5.0
- **Two Minecraft versions**: every build now makes a jar for **26.1.x** and one for **26.2** (Actions → Artifacts:
  `farmprofit-mc26.1.2` and `farmprofit-mc26.2`). If one version fails to build, the other still does.
- **Craft cost** in tooltips: what an item costs to craft from bought ingredients (each ingredient bought or crafted,
  whichever is cheaper), and whether crafting or buying is cheaper. Recipes: NEU item repository, downloaded weekly.
- **Calculator**: in Bazaar / Auction amount signs type a sum ending in `=` (`64x8=`, `10m/3=`, `(2.5k+500)*4=`) and it
  becomes the number; while typing, the result is previewed above your hotbar. Also `/calc <sum>` (copies the result).
- **HUD layout presets**: `/profit gui preset left | right | split | compact`.
- **Puzzle solvers** (Dungeons settings): Three Weirdos (tells you which chest), Blaze puzzle (lowest / highest blaze with
  arrows), and Floor 7 / Master 7 terminals (Correct all the panes, Click in order, What starts with, Select all the
  [color] items, Change all to same color — highlights what to click; you still click).
- `MODRINTH.md`: ready-made project description if you publish the mod.

## New in 4.3
- Scrollable tooltips: item descriptions taller than the screen can be scrolled with the mouse wheel while you hover
  the item in a menu. The name stays at the top and "▲ / ▼ N more" shows what's hidden. Settings → Items & areas.

## New in 4.2
- Enchantment colors in tooltips: **perfect** (absolute max, gold + ✦), **great** (above the enchanting-table max, purple),
  **good** (table max, blue), **low** (gray). Ultimate enchants stay bold. All colors (including rainbow) are in
  Settings → Items & areas. Max levels come from the public SkyKings Bot-Data file, so new enchants work automatically.
  If you also run SkyHanni, the duplicate check offers to turn one of the two off.

## New in 4.1
- Foraging starts reliably: instant log breaks (Sweep) are counted, breaks are detected from the first hit, and chopping
  5 logs anywhere starts a Foraging session even if the island isn't recognised.
- Combat HUD: kills, kills/h, most-killed mobs, profit per kill (mob names read from Hypixel's nametags).
- Grind HUDs: when most kills are one farmable mob (Zealot, Enderman/Voidling, Ghost, Blaze, Wither Skeleton,
  Magma Cube, Ice/Glacite Walker, Arachne's spiders) the HUD becomes e.g. "✦ Zealot grind" with each key drop's
  count, odds (1 per X kills) and kills since the last one, plus Special Zealots.
- The "cheap items" option is gone: items are listed one by one, and only when there are more than "Items shown"
  are the cheapest added up into one line.

## New in 4.0
- **Setup check** the first time you join SkyBlock (reopen with `/profit setup`): ✔/✖ for location, Stats and Powders
  widgets, sidebar, prices and the Hypixel Mod API, with a button that opens Hypixel's `/widget` menu.
- **Settings menu**: search box, ↺ reset next to every setting, "Reset tab", a **Hidden items** tab to count items again,
  and a Configure button in **Mod Menu** if you have it.
- **HUD editor** (`/profit gui`, or the button in Settings → HUD): every panel gets an outline; drag to move,
  middle-click to change size, right-click the title to hide or show it. `/profit gui reset` resets the layout.
  HUD settings → *Separate small panels* splits off the secret finder, Jacob's contest and "Best now";
  *Position per activity* gives the main HUD its own spot per activity. Clicking the main title toggles the all-time line.
- **Duplicates**: if SkyHanni, Skyblocker, Odin or Secret Routes already do something (price tooltips, chest profit,
  secret finder, contest reminder), you get a one-click `[Turn these off here]`.
- `/profit report` copies a diagnostics report to paste to Claude; `/profit note <text>` adds a note to the current session
  (shown in history and the CSV).
- **Profiles**: `/profit profile save <name>`, `load <name>`, `list`, `export` (copies a code), `import <code>`.
- **New-build check**: tells you when your GitHub repo has a newer successful build (Settings → General → GitHub repo).

## New in 3.1
- Combat sessions only start after real fighting: 5 mob hits within 30 s (players, NPCs and armor stands never count),
  or a slayer quest. Both are adjustable in Combat & Slayers settings.
- The HUD follows what you do: it switches as soon as you do something else (farm, chop, fish...) and when you arrive
  on a new island. Unknown log blocks on foraging islands are counted too, so Galatea woods always start a session.
- About 50 new settings: HUD look (opacity, line height, shadow, which lines show, number format),
  and per-activity options in every tab (Farming, Mining, Foraging, Fishing, Combat, Dungeons, Kuudra, Diana, Bazaar).

## New in 3.0
**Accuracy**
- Bazaar sale values now include Bazaar tax (the `Bazaar tax %` setting), so profit isn't overstated.
- Items are identified by their real SkyBlock ID once you've held them (pets, books and reforged items price correctly).
- Items you use up (potions, arrows, visitor requests) go into a separate **Spent** list instead of quietly lowering totals.
- Items from dungeon reward chests and Garden visitor menus are now counted (other menus are still ignored, so buying isn't "profit").
- **`/profit debug`** shows what's working (tab list, location, sidebar, prices, icons, scale, mouse, recognised messages).
  Hypixel messages the mod doesn't understand yet are saved to `config/farmprofit/unrecognised-messages.txt`.

**Location: install the Hypixel Mod API (recommended)**
Download *Hypixel Mod API* for Fabric 26.1 from Modrinth and put it in `mods`. The mod then gets your exact location from
Hypixel and no longer depends on the tab list's Area line. Without it, the tab list is used like before.

**New features**
- Price tooltips: hover any item for Bazaar sell/buy, lowest BIN and NPC price (toggle in HUD settings).
- Dungeon chest profit: opening a reward chest or the Croesus run view prints each chest's value, cost and profit, plus the best chest.
- Garden visitors: visitors accepted, copper (valued with `Copper value`), requested items in Spent, rewards in profit.
- Jacob's contests on the Farming HUD (next crops + countdown; "Best now" marks a crop that's in a running contest).
  Contest data is community data from the Elite Farmers API (elitebot.dev), collected from SkyHanni users.
- Mayor on the HUD of the activity they boost (from Hypixel's election data), and in `/farmprofit suggest`.
- Bazaar: warning when an order you place is far from the market (possible typo), and orders placed today in `/flips log`.
- New activities: **Kuudra** (`/kuudraprofit`, runs per hour) and **Diana** (`/dianaprofit`, burrows per hour, coins dug).
- Trophy fish broken down by type on the Fishing HUD.
- Optional all-time line on every HUD (HUD settings → Show all-time line).
- The Bazaar orders panel is its own box: drag it separately with chat open.
- `/profit export` writes `history.csv` (open in Excel/Sheets for graphs); `/profit history` shows a profit/h trend bar.

## Settings menu
Press **O** (rebind it in Options → Controls → Key Binds → "SkyBlock Profit Counter"), or use `/profit settings`,
to open a menu with **every** setting, grouped into tabs:
General, HUD, Farming, Mining, Foraging, Fishing, Combat & Slayers, Dungeons, Bazaar flipping, Items & areas.
Each activity tab also has a switch to hide that activity's HUD (tracking keeps running).
Hover a setting for an explanation. On/off settings are buttons, choices cycle when clicked, numbers accept
`10m` / `500k`, lists are comma separated and item/block maps use `name=ID, name2=ID2`.
Changes save immediately (text boxes save when you switch tab or press Done). `/flips settings` opens it too.

**For future additions:** the menu is built automatically from `Config.java`. Every new option is added there with
`@Setting(category = ..., label = ..., desc = ...)` and appears in the menu on its own; an option without the
annotation still shows up under an "Other" tab.

## Dungeons (☠ Catacombs HUD)
- Runs, average run time, runs/hour, last score, profit, profit/h and profit per run (drops, rare drops, shards).
  `/dungeonprofit` for the profit-only view, `/dungeonprofit history` for past sessions.
- **Secret finder** (no route database, so it works in every room):
  - Hypixel's own room counter from the action bar: `room 3/7 (4 left)` or `✔ room done`.
  - The 5 nearest likely secrets with an arrow (relative to where you look), distance and ▲/▼ for above/below:
    chests, levers, Wither Essence skulls, secret items lying on the floor, and bats.
  - Chests/levers/skulls you right-click are crossed off.
  - It shows *candidates* (decorative skulls and some levers aren't secrets). For full routes use Skyblocker / Secret Routes alongside this mod.
  - `/profit secrets` turns it on/off.

## "Best now" suggestions
The farming and mining HUDs show which crop / ore makes the most coins per hour right now, e.g.
`Best now: Nether Wart ~8.1M/h (you: 6.2M/h)` (a green ✔ means you're already on it).
`/farmprofit suggest` and `/miningprofit suggest` show the full ranking.
- Farming: your blocks/s (or `defaultBps` until you've farmed a minute) × base drops × (Farming Fortune + crop fortune) × best price (raw, enchanted or NPC).
- Mining: your Mining Speed vs. each block's strength (`miningEfficiency` = 0.6 for walking/aiming) × fortune × best price, only for blocks on the island you're on.
These are estimates from Bazaar trends; pests, rare drops and powder aren't included. Turn off with `showSuggestion: false`.

## Bazaar flipping (`/flips`)
Built in from the Bazaar Flip Assistant. It only reads public bazaar data and your own chat; you do all the clicking.
- `/flips [count]` – best flips right now (buy order → sell offer, margin after tax, qty, coins/h). Hover for details, **click to open the item in the Bazaar**.
- `/flips plan [items]` – splits your budget across the best few safe flips.
- **Order tracking is automatic**: place, flip, claim or cancel orders in-game and the mod reads the `[Bazaar]` chat messages.
  Open orders appear under the HUD with ✔ (best price), ✖ outbid/undercut + the price to re-list at, or "filled, claim it".
  You get a ding + chat alert the moment you're outbid or undercut (prices are checked every 20 s while you have orders).
- Profit is logged when you claim the coins from a sell offer (cost = what you paid, oldest first). `/flips log` shows today / all-time.
- `/flips orders`, `/flips remove <n>`, `/flips clear`, `/flips hud`
- `/flips settings` and `/flips set <budget|minvolume|minmargin|maxprice|tax|share|alert|top|sound> <value>`
  e.g. `/flips set budget 25m`, `/flips set tax 1`, `/flips set alert 500k` (ping for hot flips).

## Tab list widgets
The mod reads Hypixel's tab list: the **Area** line (to know where you are), **Stats** (fortune, speed,
Sweep, Magic Find...), **Powders** and **Commissions** (mining), and Forest Whispers (foraging).

## Setup (no programming tools needed)

### 1. Get the mod file built by GitHub (free, ~5 minutes)
1. Make a free account at github.com.
2. Click **+** (top right) → **New repository**, name it `farmprofit`, click **Create repository**.
3. On the next page click **uploading an existing file**. Unzip this project and drag
   **everything inside the `farmprofit` folder** into the browser. Click **Commit changes**.
   - On Mac, the `.github` folder is hidden. If it didn't upload: in your repo click
     **Add file → Create new file**, type `.github/workflows/build.yml` as the name,
     paste in the contents of that file, and commit.
4. Open the **Actions** tab. A "Build mod" run starts by itself (takes ~3-5 min).
   If nothing is running, click **Build mod → Run workflow**.
5. When it shows a green check, click the run, scroll to **Artifacts**, download
   `farmprofit-mod`, and unzip it. Inside is `farmprofit-1.0.0.jar`.

### 2. Install
1. Install Fabric Loader for Minecraft **26.1.2** (fabricmc.net/use/installer) — or use
   Prism Launcher / Modrinth App and create a Fabric 26.1.2 instance.
2. Put **Fabric API** (for 26.1.2, from Modrinth) and `farmprofit-1.0.0.jar` in your `mods` folder.
3. Launch and join Hypixel.

### 3. In game
- For the fortune line, enable the **Stats** widget in Hypixel's tab list settings
  and make sure Farming Fortune is one of the shown stats.
- Start farming. The HUD appears as soon as you break a crop.

## Settings
`.minecraft/config/farmprofit/config.json`
- `resetMinutes` – idle time before a session ends (default 15)
- `priceMode` – `"best"` (default: Bazaar instasell or NPC, whichever pays more), `"instasell"`, `"sellorder"`, `"npc"`
- `pauseSeconds` – the timer pauses after this long without activity (default 30), so AFK time doesn't lower profit/h
- `minItemValue` – items worth less than this are grouped into one "cheap items" HUD line (default 1000)
- `ignoredItems` – items that never count
- `hudX`, `hudY`, `hudMaxItems`, `hudEnabled`, `hudDetails`
- `slayerQuestCost` – coins subtracted from profit per slayer quest started (default 0)
- `miningAreas`, `foragingAreas` – tab-list Area names for those HUDs
- `extraItems`, `extraCropBlocks`, `extraOreBlocks`, `extraLogBlocks` – add things the mod doesn't know
- `showShards`, `showRareDrops`, `showCommissions`, `lowestBinUrl`

History is saved to `.minecraft/config/farmprofit/history.json` (last 200 sessions).
