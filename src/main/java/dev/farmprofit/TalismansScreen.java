package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** /talismans as a menu: the cheapest Magical Power you don't have yet, with buttons to search the AH or see the recipe. */
public final class TalismansScreen extends Screen {
    private static int count = 10;
    private static boolean showOther;
    private int page;
    private List<Accessories.Pick> picks = List.of();

    public TalismansScreen() { super(Component.literal("Next talismans")); }

    private static String color(String rarity) {
        return switch (rarity) {
            case "UNCOMMON" -> "§a";
            case "RARE" -> "§9";
            case "EPIC" -> "§5";
            case "LEGENDARY" -> "§6";
            case "MYTHIC" -> "§d";
            case "SPECIAL", "VERY SPECIAL" -> "§c";
            default -> "§f";
        };
    }

    @Override
    protected void init() {
        Config cfg = Config.get();
        int left = Math.max(10, width / 2 - 220), right = Math.min(width - 10, width / 2 + 220);
        addRenderableWidget(new StringWidget(left, 10, 300, 10, Component.literal("§6§lNext talismans §7— cheapest Magical Power you don't have"), font));

        // ---- controls ----
        int cy = 26;
        for (int n : new int[]{10, 20, 50}) {
            final int nn = n;
            addRenderableWidget(Button.builder(Component.literal((count == n ? "§e§l" : "") + "Top " + n), b -> { count = nn; page = 0; rebuildWidgets(); })
                    .bounds(left + (n == 10 ? 0 : n == 20 ? 52 : 104), cy, 48, 18).build());
        }
        Button craft = Button.builder(Component.literal("Crafting: " + (cfg.talismanUseCraft ? "§aON" : "§cOFF")), b -> {
            cfg.talismanUseCraft = !cfg.talismanUseCraft;
            Config.save();
            page = 0;
            rebuildWidgets();
        }).bounds(left + 160, cy, 90, 18).build();
        craft.setTooltip(Tooltip.create(Component.literal("Also consider crafting (and upgrading what you own) when it's cheaper than the Auction House.")));
        addRenderableWidget(craft);
        EditBox max = new EditBox(font, left + 256, cy, 80, 18, Component.literal("Max price"));
        max.setHint(Component.literal("§8max price"));
        max.setValue(cfg.talismanMaxPrice > 0 ? Fmt.coins(cfg.talismanMaxPrice).replace(",", "") : "");
        max.setTooltip(Tooltip.create(Component.literal("Skip accessories above this price, e.g. 5m or 500k. Empty = no limit. Press Refresh to apply.")));
        addRenderableWidget(max);
        Button other = Button.builder(Component.literal(showOther ? "§eOther ways" : "Other ways"), b -> { showOther = !showOther; page = 0; rebuildWidgets(); })
                .bounds(left + 408, cy, 74, 18).build();
        other.setTooltip(Tooltip.create(Component.literal("Accessories you can't buy or craft: quests, drops, events, collections. Shows how to get them.")));
        addRenderableWidget(other);
        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> {
            double v = FlipsCommand.parseAmount(max.getValue().isBlank() ? "0" : max.getValue());
            if (!Double.isNaN(v)) { cfg.talismanMaxPrice = Math.max(0, v); Config.save(); }
            page = 0;
            rebuildWidgets();
        }).bounds(left + 342, cy, 60, 18).build());

        // ---- the list ----
        int top = cy + 28, rowH = 22, bottom = height - 50;
        if (Accessories.count() == 0 || !Prices.loaded()) {
            addRenderableWidget(new StringWidget(left, top, 400, 10, Component.literal(Accessories.count() == 0
                    ? "§7Item data is still downloading (the first time takes a minute). Press Refresh shortly."
                    : "§7Prices are still loading. Press Refresh in a moment."), font));
            footer(left, right);
            return;
        }
        picks = Accessories.recommend(count);
        if (showOther) { otherList(left, right, top, rowH, bottom); return; }
        if (picks.isEmpty()) {
            addRenderableWidget(new StringWidget(left, top, 400, 10, Component.literal("§7Nothing found with a price (check the max price)."), font));
            footer(left, right);
            return;
        }
        int perPage = Math.max(1, (bottom - top) / rowH);
        int pages = (picks.size() + perPage - 1) / perPage;
        page = Math.max(0, Math.min(page, pages - 1));
        double total = 0;
        int mp = 0;
        for (Accessories.Pick p : picks) { total += p.cost(); mp += p.gain(); }

        int row = 0;
        for (int i = page * perPage; i < Math.min(picks.size(), (page + 1) * perPage); i++, row++) {
            Accessories.Pick p = picks.get(i);
            int ry = top + row * rowH;
            String name = "§8" + (i + 1) + ". " + color(p.item().rarity()) + p.item().name();
            StringWidget nameW = new StringWidget(left, ry + 5, 170, 10, Component.literal(name), font);
            nameW.setTooltip(Tooltip.create(Component.literal(color(p.item().rarity()) + p.item().name() + "\n§7" + p.item().rarity()
                    + " accessory\n§7+" + p.gain() + " Magical Power" + (p.replaces() != null ? "\n§7Upgrades your " + p.replaces() : ""))));
            addRenderableWidget(nameW);
            addRenderableWidget(new StringWidget(left + 175, ry + 5, 50, 10, Component.literal("§a+" + p.gain() + " MP"), font));
            addRenderableWidget(new StringWidget(left + 228, ry + 5, 70, 10, Component.literal("§6" + Fmt.coins(p.cost())), font));
            addRenderableWidget(new StringWidget(left + 300, ry + 5, 70, 10, Component.literal("§8" + Fmt.coins(p.cost() / p.gain()) + "/MP"), font));
            boolean ah = p.source().equals("AH"), npc = p.source().equals("NPC");
            Button go = Button.builder(Component.literal(ah ? "§eAH" : npc ? "§dNPC" : "§bRecipe"), b -> {
                        if (npc) { onClose(); wiki(p.item().name()); } else run((ah ? "ahs " : "recipe ") + p.item().name());
                    }).bounds(right - 60, ry, 60, 20).build();
            go.setTooltip(Tooltip.create(Component.literal(ah ? "Lowest BIN " + Fmt.coins(p.cost()) + ". Opens an Auction House search."
                    : npc ? "Sold by an NPC for about " + Fmt.coins(p.cost()) + ". Click for the wiki page (which NPC)."
                    : "Craft cost " + Fmt.coins(p.cost()) + " (ingredients bought). Opens the recipe.")));
            addRenderableWidget(go);
        }

        // ---- bottom ----
        int by = height - 44;
        addRenderableWidget(new StringWidget(left, by, 400, 10, Component.literal("§7Total for these " + picks.size() + ": §6"
                + Fmt.coins(total) + " §7for §a+" + mp + " MP"), font));
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); }).bounds(right - 110, by - 4, 20, 18).build()).active = page > 0;
            addRenderableWidget(new StringWidget(right - 86, by, 30, 10, Component.literal("§7" + (page + 1) + "/" + pages), font));
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); }).bounds(right - 52, by - 4, 20, 18).build()).active = page < pages - 1;
        }
        footer(left, right);
    }

    /** Accessories without a price: how to get them, with a wiki link. Sorted by Magical Power. */
    private void otherList(int left, int right, int top, int rowH, int bottom) {
        List<Accessories.Pick> other = Accessories.unbuyable;
        int perPage = Math.max(1, (bottom - top) / rowH);
        int pages = Math.max(1, (other.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        if (other.isEmpty()) addRenderableWidget(new StringWidget(left, top, 400, 10, Component.literal("§7Nothing missing that can't be bought."), font));
        int row = 0;
        for (int i = page * perPage; i < Math.min(other.size(), (page + 1) * perPage); i++, row++) {
            Accessories.Pick p = other.get(i);
            int ry = top + row * rowH;
            String hint = CraftCost.HINTS.getOrDefault(p.item().id(), "quest, drop, event or collection reward");
            StringWidget nameW = new StringWidget(left, ry + 5, 170, 10, Component.literal(color(p.item().rarity()) + p.item().name()), font);
            nameW.setTooltip(Tooltip.create(Component.literal(color(p.item().rarity()) + p.item().name() + "\n§7+" + p.gain() + " MP\n§7" + hint)));
            addRenderableWidget(nameW);
            addRenderableWidget(new StringWidget(left + 175, ry + 5, 50, 10, Component.literal("§a+" + p.gain() + " MP"), font));
            addRenderableWidget(new StringWidget(left + 228, ry + 5, right - left - 300, 10, Component.literal("§7" + hint), font));
            addRenderableWidget(Button.builder(Component.literal("Wiki"), b -> { onClose(); wiki(p.item().name()); }).bounds(right - 60, ry, 60, 20).build());
        }
        int by = height - 44;
        addRenderableWidget(new StringWidget(left, by, 400, 10, Component.literal("§7" + other.size() + " accessories you don't have that can't be bought or crafted."), font));
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); }).bounds(right - 110, by - 4, 20, 18).build()).active = page > 0;
            addRenderableWidget(new StringWidget(right - 86, by, 30, 10, Component.literal("§7" + (page + 1) + "/" + pages), font));
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); }).bounds(right - 52, by - 4, 20, 18).build()).active = page < pages - 1;
        }
        footer(left, right);
    }

    private static void wiki(String name) {
        String url = "https://hypixelskyblock.minecraft.wiki/w/" + name.replace(' ', '_');
        Tracker.say(Chat.link("§6[Talismans] §f" + name + " §7on the wiki: §a§n[open]", url));
    }

    private void footer(int left, int right) {
        int fy = height - 26;
        long scanned = Accessories.scannedAt();
        String bag = scanned == 0 ? "§eOpen every page of your Accessory Bag once so owned ones are skipped."
                : "§8Accessory Bag read " + new java.text.SimpleDateFormat("dd.MM HH:mm").format(new java.util.Date(scanned)) + ". Reopen it after buying.";
        addRenderableWidget(new StringWidget(left, fy + 5, 330, 10, Component.literal(bag), font));
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(right - 60, fy, 60, 20).build());
    }

    /** Closes the menu and runs a Hypixel command (you clicked it). */
    private void run(String command) {
        onClose();
        Object conn = Minecraft.getInstance().getConnection();
        if (Reflect.call(conn, "sendCommand", command) == Reflect.FAIL) Tracker.say("§6[Talismans] §7Type §f/" + command);
    }

    @Override
    public void onClose() { Compat.setScreen(Minecraft.getInstance(), null); }

    // ---------------- opening from a command ----------------

    private static boolean openNextTick;

    public static void requestOpen(int n) { count = n; openNextTick = true; }

    static void tick(Minecraft mc) {
        if (openNextTick && Compat.noScreen(mc)) {
            openNextTick = false;
            Compat.setScreen(mc, new TalismansScreen());
        }
    }
}
