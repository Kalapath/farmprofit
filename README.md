# SkyBlock Profit Counter – Farming & Mining (Hypixel SkyBlock, Fabric 26.1.2)

## What it shows (HUD, top-left)
- How long you've been farming
- What you're farming (the crop you've broken most) + blocks per second
- Your farming fortune (read from the tab list)
- Profit so far and estimated profit per hour (live Bazaar prices)
- Every item you've farmed and how much it's worth
- A countdown when you're idle. After 15 minutes with no crop broken / no pest
  hit or vacuumed, the session ends and is saved to the history log.

## Mining (v1.1)
A separate HUD appears automatically in each mining area:
- **Dwarven Mines** (green): mining speed/fortune, ore & block & dwarven metal fortune, Mithril Powder gained + per hour, items mined, commissions.
- **Crystal Hollows** (pink): mining speed/fortune, gemstone fortune, Gemstone + Mithril Powder, which gemstones/ores you've been mining, items, commissions.
- **Glacite Mineshaft** (aqua): mining speed/fortune, gemstone fortune, Glacite Powder, blocks mined, items.

Each area keeps its own session with its own 15-minute idle timer, so popping into a mineshaft and back
doesn't reset your Dwarven Mines session. Finished mining sessions go into the same history log.

For mining to work, Hypixel's tab list needs the **Info/Area** line (shows "Area: Dwarven Mines" etc.),
the **Stats** widget (speed/fortune) and the **Powders** widget (powder tracking).
Commissions show if the Commissions widget is enabled.

`/miningprofit` works exactly like `/farmprofit` (both act on whatever HUD you're currently seeing).
If a block isn't being counted, add it in the config under `extraOreBlocks`, e.g. `"clay": "Tungsten"`.

## Foraging (v1.3)
A green **♣ Foraging** HUD appears on foraging islands (default: Galatea and The Park; change with
`foragingAreas` in the config). It shows time, which wood you're chopping (Fig, Mangrove, Oak, ...), logs and
blocks per second, **Tree Gifts** and trees per minute (Galatea), Sweep / Foraging Fortune / Fig & Mangrove
Fortune, **Forest Whispers** gained + per hour, profit, profit/h, items chopped, rare drops and the idle timer.
`/foragingprofit` works like the other commands. Unknown log blocks can be added under `extraLogBlocks`.
Turn on the Stats widget (Sweep, Foraging Fortune) and the widget showing Forest Whispers in the tab list.

## Attribute shards (v1.4)
Shards announced in chat (from Tree Gifts, hunting, traps, mob drops...) are counted and shown under **Shards**
with their Bazaar value, and added to profit (they go to the Hunting Box, so they can't be counted twice).
The Foraging HUD always shows the shard line; other HUDs show it once you get a shard (e.g. Pest Shards while farming).
Fusing, syphoning, Bazaar and transfer messages are ignored. Toggle with `showShards` in the config.

## Rare drops, pests & pristine (v1.2)
- Every `RARE DROP!`, `VERY RARE DROP!`, `CRAZY RARE DROP!`, `PET DROP!`, `RARE CROP!` (etc.) message during a
  session is logged under **Rare drops** on the HUD and in history, with its value.
- Farming: pest kills are counted by type from the "You received ... for killing a ..." message.
- Crystal Hollows / Mineshaft: Pristine procs are counted.
- Rare drops are added to profit **unless** the same item was already counted through your inventory/sacks
  (shown as "in items"), so nothing is counted twice.
- Auction-house items are priced with lowest BIN from `lowestBinUrl` in the config; Bazaar items use the Bazaar.
  Items with no price show "?". Pets usually show "?".

## Commands
| Command | What it does |
|---|---|
| `/farmprofit` | Show the current session in chat (full item list) |
| `/farmprofit history [count]` | Show past sessions (default last 10) |
| `/farmprofit reset` | End the current session now (it still goes to history) |
| `/farmprofit hud` | Turn the HUD on/off |
| `/farmprofit move <x> <y>` | Move the HUD |
| `/farmprofit prices` | Refresh Bazaar prices now |
| `/farmprofit reload` | Reload the config file |

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
- `priceMode` – `"instasell"` (default) or `"sellorder"`
- `hudX`, `hudY`, `hudMaxItems`, `hudEnabled`
- `extraItems` – add items the mod doesn't know: `"Item Name": "BAZAAR_ID"`
- `extraCropBlocks` – add crop blocks: `"block_id": "Crop Name"`

History is saved to `.minecraft/config/farmprofit/history.json` (last 200 sessions).

## How items are counted
- Items landing in your **sacks** are read from Hypixel's `[Sacks]` chat messages (they arrive every ~30s).
- Items landing in your **inventory** are counted by watching inventory changes while you farm.
  Compacting (e.g. Wheat → Enchanted Wheat) is handled because it counts the net change.
- Items with no Bazaar price show `(?)` and count as 0 coins.

## Different Minecraft version?
Edit the four version lines at the top of `gradle.properties` and the `"minecraft"` line in
`src/main/resources/fabric.mod.json`, commit, and GitHub rebuilds it. Correct version numbers
are listed at fabricmc.net/develop.
