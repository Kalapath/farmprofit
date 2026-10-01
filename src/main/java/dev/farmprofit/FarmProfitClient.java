package dev.farmprofit;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FishingRodItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class FarmProfitClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("farmprofit");

    private static KeyMapping settingsKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("farmprofit", "main"));
        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.farmprofit.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settingsKey.consumeClick()) {
                if (client.screen == null) client.setScreen(new SettingsScreen());
            }
        });

        Config.load();
        Prices.refresh();
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("hypixel-mod-api")) HypixelLocation.init();
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> PriceTooltip.add(stack, lines));

        // Crops (farming breaks happen on the client)
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
            if (Tracker.MINING.equals(Tracker.area) || Tracker.FORAGING.equals(Tracker.area)) return;
            var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            String crop = Items.cropFor(key.getPath());
            if (crop != null) Tracker.onCropBroken(crop);
        });

        // Hitting mobs: combat / sea creatures / pests / keeps mining & foraging alive
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player == Minecraft.getInstance().player) Tracker.onAttack();
            return InteractionResult.PASS;
        });

        // Fishing rod casts/reels, and pest vacuums
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (player == Minecraft.getInstance().player) {
                var stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof FishingRodItem) {
                    Tracker.onRodUse();
                } else if (!Tracker.MINING.equals(Tracker.area) && !Tracker.FORAGING.equals(Tracker.area)) {
                    String name = Tracker.strip(stack.getHoverName().getString());
                    if (name.contains("Vacuum")) Tracker.onVacuum();
                }
            }
            return InteractionResult.PASS;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) Secrets.onActionBar(Tracker.strip(message.getString()));
            else Tracker.onChat(message);
        });

        // Right-clicking blocks: marks dungeon secrets as done, keeps the dungeon session alive
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player == Minecraft.getInstance().player && Tracker.DUNGEONS.equals(Tracker.area)) {
                Secrets.onUse(hit.getBlockPos());
                Tracker.onDungeonAction();
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(Tracker::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Tracker.endAll(false));

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("farmprofit", "hud"), (graphics, delta) -> {
                    Minecraft mc = Minecraft.getInstance();
                    Config cfg = Config.get();
                    boolean chat = mc.screen instanceof ChatScreen;
                    if (!chat) HudEditor.reset();
                    if (!cfg.hudEnabled || mc.options.hideGui || mc.player == null) return;

                    // optional scaling (quietly off if not available on this version)
                    float scale = (float) Math.max(0.5, Math.min(3.0, cfg.hudScale));
                    Object pose = Reflect.call(graphics, "pose");
                    boolean scaled = false;
                    if (Math.abs(scale - 1f) > 0.01f && Reflect.call(pose, "pushMatrix") != Reflect.FAIL) {
                        scaled = Reflect.call(pose, "scale", scale, scale) != Reflect.FAIL;
                        if (!scaled) Reflect.call(pose, "popMatrix");
                        Debug.scaleWorks = scaled;
                    }
                    float eff = scaled ? scale : 1f;

                    // panels: main HUD, then the bazaar orders panel (own position, or under the main one)
                    List<HudEditor.Box> boxes = new ArrayList<>();
                    Hud.Lines main = Hud.lines();
                    int mx = Math.round(cfg.hudX / eff), my = Math.round(cfg.hudY / eff);
                    if (!main.isEmpty()) boxes.add(layout("main", main, mx, my, mc, cfg));
                    Hud.Lines bz = Bazaar.hudLines();
                    if (!bz.isEmpty()) {
                        int bx = cfg.bazaarHudX >= 0 ? Math.round(cfg.bazaarHudX / eff) : mx;
                        int by = cfg.bazaarHudY >= 0 ? Math.round(cfg.bazaarHudY / eff)
                                : (boxes.isEmpty() ? my : my + boxes.get(0).h() + 10);
                        boxes.add(layout("bazaar", bz, bx, by, mc, cfg));
                    }
                    if (boxes.isEmpty()) { if (scaled) Reflect.call(pose, "popMatrix"); return; }

                    int[] hovered = chat ? HudEditor.update(mc, boxes, eff) : null;
                    for (int b = 0; b < boxes.size(); b++) {
                        HudEditor.Box box = boxes.get(b);
                        graphics.fill(box.x() - 3, box.y() - 3, box.x() + box.w() + 3, box.y() + box.h() + 1, chat ? 0xB0000000 : 0x90000000);
                        if (hovered != null && hovered[0] == b && hovered[1] >= 0 && box.lines().get(hovered[1]).item() != null) {
                            int hy = box.y() + hovered[1] * 10;
                            graphics.fill(box.x() - 3, hy - 1, box.x() + box.w() + 3, hy + 9, 0x40FFFFFF);
                        }
                        for (int i = 0; i < box.lines().size(); i++) {
                            Hud.HudLine l = box.lines().get(i);
                            int ly = box.y() + i * 10, tx = box.x();
                            Object icon = cfg.hudIcons ? Tracker.icon(l.item()) : null;
                            if (icon != null) {
                                drawIcon(graphics, pose, icon, box.x(), ly);
                                tx += 11;
                            }
                            graphics.text(mc.font, l.text(), tx, ly, 0xFFFFFFFF, true);
                        }
                    }
                    if (chat) {
                        HudEditor.Box last = boxes.get(boxes.size() - 1);
                        graphics.text(mc.font, "§7Drag panels to move §8| §7Right-click an item to hide it",
                                boxes.get(0).x(), last.y() + last.h() + 4, 0xFFFFFFFF, true);
                    }
                    if (scaled) Reflect.call(pose, "popMatrix");
                });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildCommand("profit", null));
            dispatcher.register(buildCommand("farmprofit", Tracker.FARMING));
            dispatcher.register(buildCommand("miningprofit", Tracker.MINING));
            dispatcher.register(buildCommand("foragingprofit", Tracker.FORAGING));
            dispatcher.register(buildCommand("fishingprofit", Tracker.FISHING));
            dispatcher.register(buildCommand("combatprofit", Tracker.COMBAT));
            dispatcher.register(buildCommand("dungeonprofit", Tracker.DUNGEONS));
            dispatcher.register(buildCommand("kuudraprofit", Tracker.KUUDRA));
            dispatcher.register(buildCommand("dianaprofit", Tracker.DIANA));
            dispatcher.register(FlipsCommand.build());
            dispatcher.register(ClientCommands.literal("profitsettings").executes(ctx -> { SettingsScreen.requestOpen(); return 1; }));
        });
        LOG.info("Profit Counter loaded");
    }

    private static HudEditor.Box layout(String id, Hud.Lines lines, int x, int y, Minecraft mc, Config cfg) {
        int w = 0;
        for (Hud.HudLine l : lines) {
            boolean icon = cfg.hudIcons && Tracker.icon(l.item()) != null;
            w = Math.max(w, mc.font.width(l.text()) + (icon ? 11 : 0));
        }
        return new HudEditor.Box(id, lines, x, y, w, lines.size() * 10);
    }

    /** Draws a 16px item icon shrunk to fit a 10px line. Silently skipped if not possible. */
    private static void drawIcon(Object graphics, Object pose, Object stack, int x, int y) {
        if (Reflect.call(pose, "pushMatrix") == Reflect.FAIL) return;
        Reflect.call(pose, "translate", (float) x, (float) (y - 1));
        Reflect.call(pose, "scale", 0.625f, 0.625f);
        Object r = Reflect.call(graphics, new String[]{"item", "renderItem", "fakeItem", "renderFakeItem"}, stack, 0, 0);
        Debug.iconsWork = r != Reflect.FAIL;
        Reflect.call(pose, "popMatrix");
    }

    // ---------- commands ----------

    private static String typeFor(String fixed) { return fixed != null ? fixed : Tracker.shownType(); }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand(String name, String fixed) {
        return ClientCommands.literal(name)
                .executes(ctx -> { showCurrent(typeFor(fixed)); return 1; })
                .then(ClientCommands.literal("reset").executes(ctx -> {
                    String type = typeFor(fixed);
                    Session s = type == null ? null : Tracker.sessions.get(type);
                    if (s == null) Tracker.say("§6[Profit] §7No " + (type == null ? "" : type + " ") + "session running.");
                    else Tracker.endSession(s, true);
                    return 1;
                }))
                .then(ClientCommands.literal("history")
                        .executes(ctx -> { showHistory(fixed, 10); return 1; })
                        .then(ClientCommands.argument("count", IntegerArgumentType.integer(1, 200))
                                .executes(ctx -> { showHistory(fixed, IntegerArgumentType.getInteger(ctx, "count")); return 1; })))
                .then(ClientCommands.literal("suggest").executes(ctx -> { suggest(typeFor(fixed)); return 1; }))
                .then(ClientCommands.literal("total").executes(ctx -> { showTotals(fixed); return 1; }))
                .then(ClientCommands.literal("copy").executes(ctx -> { copy(typeFor(fixed)); return 1; }))
                .then(ClientCommands.literal("ignore")
                        .then(ClientCommands.argument("item", StringArgumentType.greedyString()).executes(ctx -> {
                            String item = StringArgumentType.getString(ctx, "item").trim();
                            if (!Config.get().ignoredItems.contains(item)) Config.get().ignoredItems.add(item);
                            Config.save();
                            Tracker.say("§6[Profit] §7Ignoring §f" + item + "§7. Undo with /profit unignore " + item);
                            return 1;
                        })))
                .then(ClientCommands.literal("unignore")
                        .then(ClientCommands.argument("item", StringArgumentType.greedyString()).executes(ctx -> {
                            String item = StringArgumentType.getString(ctx, "item").trim();
                            boolean removed = Config.get().ignoredItems.remove(item);
                            Config.save();
                            Tracker.say("§6[Profit] §7" + (removed ? "Counting §f" + item + " §7again." : "§f" + item + " §7wasn't ignored."));
                            return 1;
                        })))
                .then(ClientCommands.literal("scale")
                        .then(ClientCommands.argument("size", DoubleArgumentType.doubleArg(0.5, 3.0)).executes(ctx -> {
                            Config.get().hudScale = DoubleArgumentType.getDouble(ctx, "size");
                            Config.save();
                            Tracker.say("§6[Profit] §7HUD scale set to §f" + Config.get().hudScale);
                            return 1;
                        })))
                .then(ClientCommands.literal("settings").executes(ctx -> { SettingsScreen.requestOpen(); return 1; }))
                .then(ClientCommands.literal("debug").executes(ctx -> { Debug.show(); return 1; }))
                .then(ClientCommands.literal("export").executes(ctx -> { exportCsv(); return 1; }))
                .then(ClientCommands.literal("secrets").executes(ctx -> {
                    Config.get().secretFinder = !Config.get().secretFinder;
                    Config.save();
                    Tracker.say("§6[Profit] §7Dungeon secret finder " + (Config.get().secretFinder ? "§aon" : "§coff"));
                    return 1;
                }))
                .then(ClientCommands.literal("icons").executes(ctx -> {
                    Config.get().hudIcons = !Config.get().hudIcons;
                    Config.save();
                    Tracker.say("§6[Profit] §7Item icons " + (Config.get().hudIcons ? "§aon" : "§coff"));
                    return 1;
                }))
                .then(ClientCommands.literal("edit").executes(ctx -> {
                    Tracker.say("§6[Profit] §7Open chat (§fT§7), then drag the HUD with the left mouse button. "
                            + "Right-click an item line to hide it.");
                    return 1;
                }))
                .then(ClientCommands.literal("hud").executes(ctx -> {
                    Config.get().hudEnabled = !Config.get().hudEnabled;
                    Config.save();
                    Tracker.say("§6[Profit] §7HUD " + (Config.get().hudEnabled ? "§aon" : "§coff"));
                    return 1;
                }))
                .then(ClientCommands.literal("details").executes(ctx -> {
                    Config.get().hudDetails = !Config.get().hudDetails;
                    Config.save();
                    Tracker.say("§6[Profit] §7HUD " + (Config.get().hudDetails
                            ? "now shows §adetails §7(stats, powder, commissions...)" : "now shows §aprofit only"));
                    return 1;
                }))
                .then(ClientCommands.literal("move")
                        .then(ClientCommands.argument("x", IntegerArgumentType.integer(0))
                                .then(ClientCommands.argument("y", IntegerArgumentType.integer(0)).executes(ctx -> {
                                    Config.get().hudX = IntegerArgumentType.getInteger(ctx, "x");
                                    Config.get().hudY = IntegerArgumentType.getInteger(ctx, "y");
                                    Config.save();
                                    Tracker.say("§6[Profit] §7HUD moved.");
                                    return 1;
                                }))))
                .then(ClientCommands.literal("prices").executes(ctx -> {
                    Prices.refresh();
                    Tracker.say("§6[Profit] §7Refreshing prices...");
                    return 1;
                }))
                .then(ClientCommands.literal("reload").executes(ctx -> {
                    Config.load();
                    Tracker.say("§6[Profit] §7Config reloaded.");
                    return 1;
                }));
    }

    private static void showCurrent(String type) {
        Session s = type == null ? null : Tracker.sessions.get(type);
        if (s == null) {
            Tracker.say("§6[Profit] §7No " + (type == null ? "" : type + " ") + "session running. §8(try /profit history)");
            return;
        }
        for (String line : Hud.profitLines(s)) Tracker.say(line);
    }

    private static void suggest(String type) {
        boolean farming = Tracker.FARMING.equals(type);
        if (!farming && !Tracker.isMiningType(type)) {
            Tracker.say("§6[Profit] §7Suggestions are for farming and mining: §f/farmprofit suggest §7or §f/miningprofit suggest");
            return;
        }
        if (!Prices.loaded()) { Tracker.say("§6[Profit] §7Prices are still loading, try again in a moment."); return; }
        var opts = farming ? Suggest.farming() : Suggest.mining();
        if (opts.isEmpty()) {
            Tracker.say("§6[Profit] §7Can't estimate yet" + (farming ? "." : ": add Mining Speed to the Stats tab widget."));
            return;
        }
        Tracker.say("§6§l[Profit] Best " + (farming ? "crops" : "ores here") + " right now §8(estimates from live prices + your stats)");
        for (int i = 0; i < Math.min(10, opts.size()); i++) {
            var o = opts.get(i);
            Tracker.say("§8" + (i + 1) + ". §a" + o.name() + " §6~" + Fmt.coins(o.perHour()) + "/h §8(sell as "
                    + o.sellAs() + ", " + String.format(java.util.Locale.US, "%.2f", o.perItem()) + " each)");
        }
        String m = Election.mayor;
        if (m != null) Tracker.say("§7Mayor: §d" + m + " §8(" + String.join(", ", Election.perks) + ")");
        if (farming) { String c = Contests.hudLine(); if (c != null) Tracker.say(c); }
        Tracker.say("§8Based on " + (farming ? "your blocks/s and Farming Fortune" : "your Mining Speed and fortune")
                + "; pests, rare drops and powder aren't included.");
    }

    private static void exportCsv() {
        try {
            StringBuilder csv = new StringBuilder("date,activity,main,active_minutes,profit,profit_per_hour,runs_or_bosses\n");
            var fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (Session s : History.all()) {
                String type = Tracker.normalType(s.type);
                int count = s.runs > 0 ? s.runs : (Tracker.COMBAT.equals(type) ? s.totalBreaks() : 0);
                csv.append(fmt.format(new java.util.Date(s.start))).append(',').append(type).append(",\"")
                        .append(String.valueOf(s.mainCrop).replace("\"", "'")).append("\",")
                        .append(s.durationMs(0) / 60000).append(',').append(Math.round(s.profit)).append(',')
                        .append(Math.round(s.profitPerHour)).append(',').append(count).append('\n');
            }
            java.nio.file.Path file = Config.DIR.resolve("history.csv");
            java.nio.file.Files.writeString(file, csv.toString());
            Tracker.say("§6[Profit] §7Exported " + History.all().size() + " sessions to §f" + file.toAbsolutePath()
                    + " §7(open it in Excel / Google Sheets for graphs).");
        } catch (Exception e) {
            Tracker.say("§6[Profit] §cExport failed: " + e.getMessage());
        }
    }

    /** "▂▃▅▇▆" style bar of profit/h over sessions, oldest to newest. */
    private static String sparkline(List<Session> sessions) {
        String bars = "▁▂▃▄▅▆▇█";
        double max = 0;
        for (Session s : sessions) max = Math.max(max, s.profitPerHour);
        if (max <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (Session s : sessions) sb.append(bars.charAt((int) Math.min(7, Math.max(0, Math.round(s.profitPerHour / max * 7)))));
        return sb.toString();
    }

    private static void showTotals(String fixed) {
        var all = Totals.all();
        if (all.isEmpty()) {
            Tracker.say("§6[Profit] §7No finished sessions yet.");
            return;
        }
        Tracker.say("§6§l[Profit] Lifetime totals:");
        for (var e : all.entrySet()) {
            if (fixed != null && !fixed.equals(e.getKey())) continue;
            var t = e.getValue();
            double h = t.ms / 3_600_000.0;
            Tracker.say(" " + Hud.title(e.getKey()).replace("§l", "").replaceAll(" §7\\(.*\\)", "")
                    + " §f" + Fmt.duration(t.ms) + " §7in " + t.sessions + " sessions, §6" + Fmt.coins(t.profit)
                    + " §7(§6" + (h > 0 ? Fmt.coins(t.profit / h) : "0") + "/h§7)");
        }
    }

    private static void copy(String type) {
        Session s = type == null ? null : Tracker.sessions.get(type);
        if (s == null) { Tracker.say("§6[Profit] §7Nothing to copy."); return; }
        long now = System.currentTimeMillis();
        String text = s.type + ": " + Fmt.coins(s.value()) + " coins in " + Fmt.duration(s.durationMs(now))
                + " (" + Fmt.coins(s.perHour(now)) + "/h)";
        try {
            Minecraft mc = Minecraft.getInstance();
            Object kb = mc.getClass().getField("keyboardHandler").get(mc);
            kb.getClass().getMethod("setClipboard", String.class).invoke(kb, text);
            Tracker.say("§6[Profit] §7Copied: §f" + text);
        } catch (Exception e) {
            Tracker.say("§6[Profit] §7Couldn't copy, here it is: §f" + text);
        }
    }

    private static boolean matches(Session s, String fixed) {
        if (fixed == null) return true;
        String t = s.type == null ? Tracker.FARMING : s.type;
        if (Tracker.MINING.equals(fixed)) return Tracker.isMiningType(t);
        return fixed.equals(t);
    }

    private static void showHistory(String fixed, int count) {
        List<Session> all = History.all().stream().filter(s -> matches(s, fixed)).toList();
        if (all.isEmpty()) {
            Tracker.say("§6[Profit] §7No finished " + (fixed == null ? "" : fixed.toLowerCase() + " ") + "sessions yet.");
            return;
        }
        Tracker.say("§6§l[Profit] Last " + Math.min(count, all.size()) + (fixed == null ? "" : " " + fixed.toLowerCase()) + " sessions:");
        var fmt = new java.text.SimpleDateFormat("dd.MM HH:mm");
        for (int i = all.size() - 1; i >= Math.max(0, all.size() - count); i--) {
            Session s = all.get(i);
            String type = s.type == null ? Tracker.FARMING : s.type;
            Tracker.say("§8" + fmt.format(new java.util.Date(s.start)) + " " + Hud.title(type).replace("§l", "")
                    + " §a" + s.mainCrop + " §7" + Fmt.duration(s.durationMs(0)) + " §6" + Fmt.coins(s.profit)
                    + " §7(§6" + Fmt.coins(s.profitPerHour) + "/h§7)");
            if (s.shards != null && !s.shards.isEmpty()) Tracker.say(list("   §bShards: ", s.shards));
            if (s.rareDrops != null && !s.rareDrops.isEmpty()) Tracker.say(list("   §dRare: ", s.rareDrops));
        }
        List<Session> recent = all.subList(Math.max(0, all.size() - 30), all.size());
        String spark = sparkline(recent);
        if (!spark.isEmpty()) Tracker.say("§7Profit/h trend (last " + recent.size() + "): §e" + spark);
        Tracker.say("§8Full details: /profit export (CSV) or .minecraft/config/farmprofit/history.json");
    }

    private static String list(String prefix, java.util.Map<String, Integer> map) {
        StringBuilder r = new StringBuilder(prefix);
        int n = 0;
        for (var e : map.entrySet()) {
            if (n++ > 0) r.append("§7, ").append(prefix.contains("§b") ? "§b" : "§d");
            r.append(e.getValue()).append("x ").append(e.getKey());
        }
        return r.toString();
    }
}
