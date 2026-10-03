package dev.farmprofit;

import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

import dev.farmprofit.MenuScreen.Page;
import dev.farmprofit.MenuScreen.Row;
import dev.farmprofit.MenuScreen.Tab;

/**
 * Heart of the Mountain / Heart of the Forest guides for different goals.
 * Compiled from the Hypixel SkyBlock Wiki (Mining / Foraging guides, updated 2026) and recent forum threads,
 * preferring advice written after the 2024 HOTM rework and the 2026 Torrhus (HOTF) update.
 */
public final class Guides {

    private static Page page(String[][] rows, String... footer) {
        List<Row> out = new ArrayList<>();
        for (String[] r : rows) out.add(new Row(r[0], r[1]));
        return new Page(new String[]{"", ""}, new int[]{150, 300}, out, List.of(), List.of(footer));
    }

    public static Screen hotm(Screen parent) {
        return new MenuScreen("Heart of the Mountain guide", List.of(
                new Tab("Powder grinding", () -> page(new String[][]{
                        {"§eGoal", "§fGemstone powder from Crystal Hollows treasure chests (mining Hard Stone)."},
                        {"§a1. Great Explorer", "More treasure chests and fewer lockpicks needed. Everyone's first priority."},
                        {"§a2. Powder Buff", "More powder from everything (HOTM 7)."},
                        {"§a3. Mining Spread", "Breaks nearby Hard Stone too = more chests. (Older guides call this Mole; it was reworked in the 2024 HOTM update.)"},
                        {"§a4. Mining Fortune", "Players report Powder Buff + Mining Fortune as the bare necessities, the rest optional."},
                        {"§bAbility", "Maniac Miner (more fortune) or Sheer Force (more blocks broken, so more chests)."},
                        {"§bPets", "Snail (fortune from speed + spread), Armadillo (spread) or Legendary Scatha (fortune + powder). Mithril / Glacite Golem also boost powder."},
                        {"§bTip", "Don't put powder into Sky Mall while powder grinding. Use SkyAssist's treasure chest helper (box + timer + lockpick)."},
                }, "§8Sources: Hypixel forum threads written after the HOTM rework (2024–2026). Advice to 'max Mole' is from before the rework.")),
                new Tab("Gemstone mining", () -> page(new String[][]{
                        {"§eGoal", "§fMining gemstones for coins (Crystal Hollows / Mineshafts)."},
                        {"§a1. Mining Speed + Mining Fortune", "Max both, including the second-row versions (Mining Speed II / Mining Fortune II)."},
                        {"§a2. Professional", "Extra Mining Speed while mining gemstones."},
                        {"§a3. Gemstone Infusion (ability)", "Boosts gemstone fortune and mining speed while active."},
                        {"§a4. Powder Buff", "Still worth it, since gemstone mining gives powder too."},
                        {"§bPowder to start", "Community advice (2026): roughly 14m gemstone + 8m mithril powder, plus about 6m glacite powder for general gemstone mining."},
                        {"§bGear", "Titanium Drill DR-X655, Divan's armor with Jade gems, Legendary Bal / Scatha / Glacite Golem pet, perfect gemstones."},
                        {"§bMax tree", "About 8.5m mithril / 26m gemstone / 32m glacite powder in total."},
                }, "§8Sources: Hypixel forum 'Mining help' (2026) and 'New optimal HOTM tree' (after the rework).")),
                new Tab("Mithril mining", () -> page(new String[][]{
                        {"§eGoal", "§fMining Mithril (Dwarven Mines events, Crystal Hollows mithril)."},
                        {"§aMithril powder into", "Efficient Miner, Seasoned Mineman, Mining Fortune I and Mining Speed I (max all four, about 9.8m mithril powder)."},
                        {"§aGemstone powder into", "Mining Fortune II, Mining Speed II and Powder Buff (about 11–17m gemstone powder)."},
                        {"§bMagma Fields mithril", "Minimum about 7.7m mithril + 7.4m gemstone powder: Efficient Miner, Mining Speed I, Seasoned Mineman, Powder Buff, Mining Speed II."},
                        {"§bSetup", "Dimensional armor, royal equipment, Mithril Golem, T2 Mithril drill."},
                }, "§8Source: Hypixel SkyBlock Wiki, Tutorial: Mining Guide (updated 2026).")),
                new Tab("Glacite & Mineshafts", () -> page(new String[][]{
                        {"§eGoal", "§fGlacite powder, Glacite Tunnels commissions and Glacite Mineshafts."},
                        {"§a1. Surveyor (HOTM 8)", "Max it: up to +15% chance to find a Mineshaft (+15.5% with Blue Cheese Goblin Omelette)."},
                        {"§a2. Mineshaft perks", "Then the perks that help inside Mineshafts (mining speed there, corpse loot)."},
                        {"§bGetting glacite powder", "Spam Glacite Tunnels commissions, fossils and corpses (Glacite Golem pet helps with corpses)."},
                        {"§bPowder", "About 6m glacite powder for general gemstone mining, 24m+ for serious mineshaft mining."},
                }, "§8Sources: Wiki Mining Guide (2026), Hypixel forum 'Mining help' (2026).")),
                new Tab("Getting started", () -> page(new String[][]{
                        {"§eGoal", "§fHOTM 1–6, before you have much powder."},
                        {"§a1. Mining Speed", "First perk to level."},
                        {"§a2. Mining Fortune", "Second."},
                        {"§a3. Daily Powder", "The first ore each day gives +500 powder × your HOTM tier: free powder."},
                        {"§a4. Efficient Miner, Seasoned Mineman", "More drops and more Mining XP."},
                        {"§bHOTM XP", "Commissions in the Dwarven Mines / Crystal Hollows level your HOTM fastest."},
                        {"§bWhat next", "With no powder, grind powder first (Powder grinding tab), then switch the tree to what you want to do."},
                }, "§8There's no single best tree: reset it for each goal. Sources: wiki + forum, current as of 2026."))
        ), 0, parent).searchable();
    }

    public static Screen hotf(Screen parent) {
        return new MenuScreen("Heart of the Forest guide", List.of(
                new Tab("Forest Whispers (Fig)", () -> page(new String[][]{
                        {"§eGoal", "§fForest Whispers on Moonglade Marsh (Galatea)."},
                        {"§a1. Sweep", "The most important stat: more Sweep = trees fall faster = more whispers. Max the Sweep perk first."},
                        {"§a2. Center of the Forest", "More Sweep (%) and more whispers per Tree Gift / log."},
                        {"§a3. Tree Whisperer", "Extra Forest Whispers per Tree Gift. Whispers spent on it can't be refunded."},
                        {"§a4. Lottery", "Random buff to Fig / Mangrove / Helix Fortune or Sweep."},
                        {"§bMethod", "Cut small Fig trees as fast as you can. At 600+ Sweep, the tall Fig trees can be worth it too."},
                        {"§bGear", "Fig Armor (Groovy), David's Cloak + Mangrove equipment, Figstone Splitter (Moonglade reforge), Jade Dragon pet; boosters on everything."},
                        {"§cAvoid", "Ricochet / homing axe perks: players report they still often don't work right."},
                }, "§8Sources: Hypixel forum 'Best Heart of the Forest setup' (2026) and 'Most optimal HOTF tree at HOTF 7'.")),
                new Tab("Desert Whispers (Helix)", () -> page(new String[][]{
                        {"§eGoal", "§fTorrhus Canyon: Helix trees and Desert Whispers (HOTF tier 4+ perks cost Desert Whispers)."},
                        {"§a1. Reset at tier 4", "Wiki's optimal tree: Sweep, Foraging Fortune, Luck of the Forest, Hunter's Luck, 250 Gifts, Iron Lungs, Foraging Madness."},
                        {"§a2. Sweep again", "Helix trees are much tougher than Fig, so Sweep matters even more here."},
                        {"§a3. Forest Speed", "Up to +50 Sweep when you reach 500 Speed: only if you build Speed."},
                        {"§bMethod", "On Torrhus Canyon, chopping Helix trees is by far the best strategy (wiki)."},
                        {"§bGear", "Helix Chopper with Moonglade reforge, Helix Armor with Groovy reforge."},
                        {"§bPersonal Best", "Each wood's Personal Best perk gives up to +10% Sweep (all three: +30%) from contest collection."},
                }, "§8Source: Hypixel SkyBlock Wiki, Tutorial: Foraging Guide (updated after the August 2026 Torrhus update).")),
                new Tab("HOTF basics", () -> page(new String[][]{
                        {"§eHow it works", "§fTiers give Tokens of the Forest (unlock perks); perks are levelled with whispers."},
                        {"§bWhispers", "Tiers 1–3: Forest Whispers. Tiers 4+: Desert Whispers."},
                        {"§bHOTF XP", "Opening Tree Gifts on Galatea and Starlyn Contests."},
                        {"§bOrder", "Put a little into every perk first (first levels are cheap), then max the Sweep perks, then fortune."},
                        {"§bLuck of the Forest", "More Tree Gift loot: decent once Sweep is good."},
                }, "§8Sources: Hypixel SkyBlock Wiki (Heart of the Forest, 2026) and forum threads."))
        ), 0, parent).searchable();
    }

    private Guides() {}
}
