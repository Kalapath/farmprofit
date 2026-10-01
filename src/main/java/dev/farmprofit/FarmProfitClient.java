package dev.farmprofit;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FarmProfitClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("farmprofit");

    @Override
    public void onInitializeClient() {
        Config.load();
        Prices.refresh();

        // Crop broken
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
            var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            String crop = Items.cropFor(key.getPath());
            if (crop != null) Tracker.onCropBroken(crop);
        });

        // Hitting a mob (pests) while a session is running
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player == Minecraft.getInstance().player && Tracker.current != null) Tracker.onPestAction();
            return InteractionResult.PASS;
        });

        // Using a vacuum (pests)
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (player == Minecraft.getInstance().player) {
                String name = Tracker.strip(player.getItemInHand(hand).getHoverName().getString());
                if (name.contains("Vacuum")) Tracker.onPestAction();
            }
            return InteractionResult.PASS;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) Tracker.onChat(message);
        });

        ClientTickEvents.END_CLIENT_TICK.register(Tracker::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Tracker.endSession(false));

        // HUD
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("farmprofit", "hud"), (graphics, delta) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (!Config.get().hudEnabled || mc.options.hideGui || mc.player == null) return;
                    List<String> lines = hudLines();
                    int x = Config.get().hudX, y = Config.get().hudY, w = 0;
                    for (String l : lines) w = Math.max(w, mc.font.width(l));
                    graphics.fill(x - 3, y - 3, x + w + 3, y + lines.size() * 10 + 1, 0x90000000);
                    for (int i = 0; i < lines.size(); i++) {
                        graphics.text(mc.font, lines.get(i), x, y + i * 10, 0xFFFFFFFF, true);
                    }
                });

        registerCommands();
        LOG.info("Farm Profit Counter loaded");
    }

    // ---------- HUD text ----------

    static List<String> hudLines() {
        List<String> out = new ArrayList<>();
        out.add("§6§lFarming Profit");
        Session s = Tracker.current;
        if (s == null) {
            out.add("§7Break a crop to start tracking");
            return out;
        }
        long now = System.currentTimeMillis();
        String crop = s.mainCrop();
        out.add("§7Time: §f" + Fmt.duration(s.durationMs(now)));
        out.add("§7Farming: §a" + crop + " §8(" + Fmt.num(s.totalBreaks()) + " broken, "
                + String.format(java.util.Locale.US, "%.1f", s.breaksPerSecond(now)) + " BPS)");
        String f = Tracker.fortuneText(crop);
        out.add("§7Fortune: " + (f == null ? "§8enable the Stats tab widget" : "§6" + f));
        if (!Prices.loaded()) out.add("§cLoading Bazaar prices...");
        out.add("§7Profit: §6" + Fmt.coins(s.value()) + " coins");
        out.add("§7Profit/h: §6" + (s.durationMs(now) < 60_000 ? "§8wait 1 min" : Fmt.coins(s.perHour(now)) + "/h"));

        var items = new ArrayList<>(s.items.entrySet());
        items.sort((a, b) -> Double.compare(b.getValue() * Prices.price(b.getKey()), a.getValue() * Prices.price(a.getKey())));
        if (!items.isEmpty()) out.add("§7Farmed:");
        int max = Config.get().hudMaxItems;
        for (int i = 0; i < Math.min(max, items.size()); i++) out.add(itemLine(items.get(i)));
        if (items.size() > max) out.add("§8 ...and " + (items.size() - max) + " more (/farmprofit)");

        long idle = Tracker.idleMs();
        if (idle > 20_000) out.add("§eIdle - resets in " + Fmt.clock(Tracker.resetMs() - idle));
        return out;
    }

    static String itemLine(Map.Entry<String, Long> e) {
        double v = e.getValue() * Prices.price(e.getKey());
        String sign = e.getValue() > 0 ? "+" : "";
        return " §f" + sign + Fmt.num(e.getValue()) + " §a" + e.getKey() + " §8(" + (v == 0 ? "?" : Fmt.coins(v)) + ")";
    }

    // ---------- commands ----------

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("farmprofit")
                        .executes(ctx -> { showCurrent(); return 1; })
                        .then(ClientCommands.literal("hud").executes(ctx -> {
                            Config.get().hudEnabled = !Config.get().hudEnabled;
                            Config.save();
                            Tracker.say("§6[FarmProfit] §7HUD " + (Config.get().hudEnabled ? "§aon" : "§coff"));
                            return 1;
                        }))
                        .then(ClientCommands.literal("reset").executes(ctx -> {
                            if (Tracker.current == null) Tracker.say("§6[FarmProfit] §7No session running.");
                            else Tracker.endSession(true);
                            return 1;
                        }))
                        .then(ClientCommands.literal("history")
                                .executes(ctx -> { showHistory(10); return 1; })
                                .then(ClientCommands.argument("count", IntegerArgumentType.integer(1, 200))
                                        .executes(ctx -> { showHistory(IntegerArgumentType.getInteger(ctx, "count")); return 1; })))
                        .then(ClientCommands.literal("move")
                                .then(ClientCommands.argument("x", IntegerArgumentType.integer(0))
                                        .then(ClientCommands.argument("y", IntegerArgumentType.integer(0)).executes(ctx -> {
                                            Config.get().hudX = IntegerArgumentType.getInteger(ctx, "x");
                                            Config.get().hudY = IntegerArgumentType.getInteger(ctx, "y");
                                            Config.save();
                                            Tracker.say("§6[FarmProfit] §7HUD moved.");
                                            return 1;
                                        }))))
                        .then(ClientCommands.literal("prices").executes(ctx -> {
                            Prices.refresh();
                            Tracker.say("§6[FarmProfit] §7Refreshing Bazaar prices...");
                            return 1;
                        }))
                        .then(ClientCommands.literal("reload").executes(ctx -> {
                            Config.load();
                            Tracker.say("§6[FarmProfit] §7Config reloaded.");
                            return 1;
                        }))
        ));
    }

    private static void showCurrent() {
        Session s = Tracker.current;
        if (s == null) {
            Tracker.say("§6[FarmProfit] §7No session running. Break a crop to start. §8(/farmprofit history)");
            return;
        }
        for (String line : hudLines()) {
            if (line.startsWith("§8 ...and")) continue;
            Tracker.say(line);
        }
        // full item list in chat
        var items = new ArrayList<>(s.items.entrySet());
        int shown = Math.min(Config.get().hudMaxItems, items.size());
        if (items.size() > shown) {
            items.sort((a, b) -> Double.compare(b.getValue() * Prices.price(b.getKey()), a.getValue() * Prices.price(a.getKey())));
            for (int i = shown; i < items.size(); i++) Tracker.say(itemLine(items.get(i)));
        }
    }

    private static void showHistory(int count) {
        List<Session> all = History.all();
        if (all.isEmpty()) {
            Tracker.say("§6[FarmProfit] §7No finished sessions yet.");
            return;
        }
        Tracker.say("§6§l[FarmProfit] Last " + Math.min(count, all.size()) + " sessions:");
        var fmt = new java.text.SimpleDateFormat("dd.MM HH:mm");
        for (int i = all.size() - 1; i >= Math.max(0, all.size() - count); i--) {
            Session s = all.get(i);
            Tracker.say("§8" + fmt.format(new java.util.Date(s.start)) + " §a" + s.mainCrop
                    + " §7" + Fmt.duration(s.durationMs(0)) + " §6" + Fmt.coins(s.profit)
                    + " §7(§6" + Fmt.coins(s.profitPerHour) + "/h§7)"
                    + (s.fortune != null ? " §8" + s.fortune : ""));
        }
        Tracker.say("§8Full details: .minecraft/config/farmprofit/history.json");
    }
}
