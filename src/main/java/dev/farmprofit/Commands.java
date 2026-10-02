package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.farmprofit.MenuScreen.Action;
import dev.farmprofit.MenuScreen.Page;
import dev.farmprofit.MenuScreen.Row;
import dev.farmprofit.MenuScreen.Tab;

/** Every command with a short explanation, as a menu (/profit help, Settings, or the main menu). */
public final class Commands {
    /** usage, what it does, and whether it can run with nothing typed after it */
    private record Cmd(String usage, String what, boolean runnable) {}

    private static final Map<String, List<Cmd>> GROUPS = new LinkedHashMap<>();

    private static void add(String group, String usage, String what) {
        GROUPS.computeIfAbsent(group, g -> new ArrayList<>()).add(new Cmd(usage, what, !usage.contains("<") && !usage.contains("[") && !usage.contains("|")));
    }

    static {
        add("Main", "/skyassist", "Main menu with everything (also the P key or /profit menu).");
        add("Main", "/skyassist help", "This list (also /profit help).");
        add("Main", "/profit settings", "All settings, with search (also the O key or /profitsettings).");
        add("Main", "/profit", "Current session: items, profit, costs (menu).");
        add("Main", "/profit chat", "Current session printed in chat.");
        add("Main", "/profit history", "Past sessions by activity (menu).");
        add("Main", "/profit history chat", "Last 10 sessions in chat.");
        add("Main", "/profit total", "Lifetime totals per activity (menu).");
        add("Main", "/profit suggest", "Best crop / ore to farm right now (menu).");

        add("Sessions", "/profit reset", "Ends the current session now and saves it to history.");
        add("Sessions", "/profit note <text>", "Adds a note to the current session (shown in history).");
        add("Sessions", "/profit copy", "Copies \"profit in time (per hour)\" to the clipboard.");
        add("Sessions", "/profit ignore <item>", "Stops counting an item (or right-click it on the HUD with chat open).");
        add("Sessions", "/profit unignore <item>", "Counts an item again (or Settings → Hidden items).");
        add("Sessions", "/profit export", "Saves all history to history.csv for Excel / Google Sheets.");
        add("Sessions", "/farmprofit, /miningprofit, /foragingprofit", "Same commands, but for that activity only.");
        add("Sessions", "/fishingprofit, /combatprofit, /dungeonprofit", "Same commands, but for that activity only.");
        add("Sessions", "/kuudraprofit, /dianaprofit", "Same commands, but for that activity only.");

        add("HUD", "/profit gui", "HUD editor: drag panels, middle-click to resize, right-click a title to hide.");
        add("HUD", "/profit gui preset <left|right|split|compact>", "Ready-made layout for all panels.");
        add("HUD", "/profit gui reset", "Puts every panel back to the default spot.");
        add("HUD", "/profit hud", "Turns the whole HUD on or off.");
        add("HUD", "/profit details", "Switches the HUD between full details and profit only.");
        add("HUD", "/profit icons", "Item icons on the HUD on or off.");
        add("HUD", "/profit scale <0.5-3>", "Size of the main HUD panel.");
        add("HUD", "/profit move <x> <y>", "Moves the main HUD panel to an exact spot.");
        add("HUD", "/profit secrets", "Dungeon secret finder on or off.");

        add("Bazaar", "/flips", "Flip finder, plan, your orders and flip profit (menu).");
        add("Bazaar", "/flips chat", "Best flips in chat (click one to open it in the Bazaar).");
        add("Bazaar", "/flips plan", "Splits your budget over the best safe flips (menu).");
        add("Bazaar", "/flips orders", "Your tracked orders and whether you're outbid / undercut (menu).");
        add("Bazaar", "/flips log", "Profit from your finished flips (menu).");
        add("Bazaar", "/flips remove <number>", "Removes a stale order from the list.");
        add("Bazaar", "/flips clear", "Clears the order list (the profit log stays).");
        add("Bazaar", "/flips hud", "Orders panel on the HUD on or off.");
        add("Bazaar", "/flips settings", "Flip settings (budget, tax, volume...).");
        add("Bazaar", "/flips set <setting> <value>", "Change one flip setting, e.g. /flips set budget 25m.");

        add("Tools", "/talismans", "Cheapest Magical Power you don't have yet (menu).");
        add("Tools", "/talismans chat", "The same list in chat.");
        add("Tools", "/calc <sum>", "Calculator: 64x8, 10m/3, (2.5k+500)*4. Copies the result.");
        add("Tools", "In a Bazaar / AH sign: 64x8=", "Type a sum ending in = and it becomes the number.");
        add("Tools", "Chat open (T) over the HUD", "Drag panels to move them, right-click an item to stop counting it.");

        add("Settings & help", "/profit setup", "First-time check: what the mod can see and what to turn on.");
        add("Settings & help", "/profit debug", "What's working (location, prices, overlays, recognised messages).");
        add("Settings & help", "/profit report", "Copies a diagnostics report to paste when asking for help.");
        add("Settings & help", "/profit dedupe", "Turns off features that SkyHanni / Skyblocker / Odin already do.");
        add("Settings & help", "/profit prices", "Downloads fresh prices now.");
        add("Settings & help", "/profit reload", "Reloads config.json after editing it by hand.");
        add("Settings & help", "/profit profile save <name>", "Saves all settings as a named profile.");
        add("Settings & help", "/profit profile load <name>", "Loads a saved profile.");
        add("Settings & help", "/profit profile list", "Shows your saved profiles.");
        add("Settings & help", "/profit profile export", "Copies your settings as a code to share.");
        add("Settings & help", "/profit profile import <code>", "Loads settings from a shared code.");
    }

    public static Screen screen(Screen parent) {
        List<Tab> tabs = new ArrayList<>();
        for (var g : GROUPS.entrySet()) {
            tabs.add(new Tab(g.getKey(), () -> {
                List<Row> rows = new ArrayList<>();
                for (Cmd c : g.getValue()) {
                    List<Action> buttons = new ArrayList<>();
                    if (c.usage().startsWith("/")) {
                        String cmd = c.usage().split(",")[0].trim();
                        if (c.runnable()) buttons.add(new Action("§aRun", "Runs " + cmd, () -> MenuScreen.runCommand(cmd.substring(1))));
                        else buttons.add(new Action("§eType", "Opens chat with " + typed(cmd) + " filled in.", () -> openChat(typed(cmd))));
                    }
                    rows.add(new Row(new String[]{"§f" + c.usage(), "§7" + c.what()}, null, buttons));
                }
                return new Page(new String[]{"Command", "What it does"}, new int[]{200, 230}, rows, List.of(),
                        List.of("§8Keys: §fP §8main menu, §fO §8settings (change them in Options → Controls → Key Binds)."));
            }));
        }
        return new MenuScreen("Commands", tabs, 0, parent);
    }

    /** "/profit note <text>" -> "/profit note " so you can type the rest. */
    private static String typed(String usage) {
        int cut = usage.length();
        for (String m : new String[]{"<", "["}) { int i = usage.indexOf(m); if (i >= 0) cut = Math.min(cut, i); }
        return usage.substring(0, cut).trim() + " ";
    }

    /** Opens chat with text already typed. */
    private static void openChat(String text) {
        Minecraft mc = Minecraft.getInstance();
        try {
            Class<?> cls = Class.forName("net.minecraft.client.gui.screens.ChatScreen");
            for (var c : cls.getConstructors()) {
                Object[] args = switch (c.getParameterCount()) { case 1 -> new Object[]{text}; case 2 -> new Object[]{text, false}; default -> null; };
                if (args == null) continue;
                try { Compat.setScreen(mc, c.newInstance(args)); return; } catch (Exception ignored) {}
            }
        } catch (Throwable ignored) {}
        Compat.setScreen(mc, null);
        Tracker.say("§6[SkyAssist] §7Type §f" + text);
    }

    private Commands() {}
}
