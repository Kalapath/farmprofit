package dev.farmprofit;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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

import java.util.List;

public final class FarmProfitClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("farmprofit");

    @Override
    public void onInitializeClient() {
        Config.load();
        Prices.refresh();

        // Crop broken (farming breaks happen on the client, so this event works)
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
            if (Tracker.area != null) return; // mining is detected separately in Tracker
            var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            String crop = Items.cropFor(key.getPath());
            if (crop != null) Tracker.onCropBroken(crop);
        });

        // Hitting mobs (pests, mineshaft mobs) keeps the running session alive
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player == Minecraft.getInstance().player) Tracker.onAttack();
            return InteractionResult.PASS;
        });

        // Using a vacuum (pests)
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (player == Minecraft.getInstance().player && Tracker.area == null) {
                String name = Tracker.strip(player.getItemInHand(hand).getHoverName().getString());
                if (name.contains("Vacuum")) Tracker.onVacuum();
            }
            return InteractionResult.PASS;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) Tracker.onChat(message);
        });

        ClientTickEvents.END_CLIENT_TICK.register(Tracker::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Tracker.endAll(false));

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("farmprofit", "hud"), (graphics, delta) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (!Config.get().hudEnabled || mc.options.hideGui || mc.player == null) return;
                    List<String> lines = Hud.lines();
                    if (lines.isEmpty()) return;
                    int x = Config.get().hudX, y = Config.get().hudY, w = 0;
                    for (String l : lines) w = Math.max(w, mc.font.width(l));
                    graphics.fill(x - 3, y - 3, x + w + 3, y + lines.size() * 10 + 1, 0x90000000);
                    for (int i = 0; i < lines.size(); i++) {
                        graphics.text(mc.font, lines.get(i), x, y + i * 10, 0xFFFFFFFF, true);
                    }
                });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildCommand("farmprofit"));
            dispatcher.register(buildCommand("miningprofit"));
            dispatcher.register(buildCommand("foragingprofit"));
        });
        LOG.info("Profit Counter loaded");
    }

    // ---------- commands ----------

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand(String name) {
        return ClientCommands.literal(name)
                .executes(ctx -> { showCurrent(); return 1; })
                .then(ClientCommands.literal("hud").executes(ctx -> {
                    Config.get().hudEnabled = !Config.get().hudEnabled;
                    Config.save();
                    Tracker.say("§6[Profit] §7HUD " + (Config.get().hudEnabled ? "§aon" : "§coff"));
                    return 1;
                }))
                .then(ClientCommands.literal("reset").executes(ctx -> {
                    Session s = Tracker.shown();
                    if (s == null) Tracker.say("§6[Profit] §7No " + Tracker.shownType() + " session running.");
                    else Tracker.endSession(s, true);
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
                                    Tracker.say("§6[Profit] §7HUD moved.");
                                    return 1;
                                }))))
                .then(ClientCommands.literal("prices").executes(ctx -> {
                    Prices.refresh();
                    Tracker.say("§6[Profit] §7Refreshing Bazaar prices...");
                    return 1;
                }))
                .then(ClientCommands.literal("reload").executes(ctx -> {
                    Config.load();
                    Tracker.say("§6[Profit] §7Config reloaded.");
                    return 1;
                }));
    }

    private static void showCurrent() {
        Session s = Tracker.shown();
        if (s == null) {
            Tracker.say("§6[Profit] §7No " + Tracker.shownType() + " session running. §8(/farmprofit history)");
            return;
        }
        for (String line : Hud.lines()) {
            if (!line.startsWith("§8 ...and")) Tracker.say(line);
        }
        var items = Hud.sortedItems(s);
        for (int i = Config.get().hudMaxItems; i < items.size(); i++) Tracker.say(Hud.itemLine(items.get(i)));
        if (s.shards.size() > 4) {
            Tracker.say("§7All shards:");
            for (var e : s.shards.entrySet()) Tracker.say(Hud.shardLine(e));
        }
        if (s.rareDrops.size() > 4) {
            Tracker.say("§7All rare drops:");
            for (var d : s.rareDrops.entrySet()) Tracker.say(Hud.rareLine(s, d));
        }
    }

    private static void showHistory(int count) {
        List<Session> all = History.all();
        if (all.isEmpty()) {
            Tracker.say("§6[Profit] §7No finished sessions yet.");
            return;
        }
        Tracker.say("§6§l[Profit] Last " + Math.min(count, all.size()) + " sessions:");
        var fmt = new java.text.SimpleDateFormat("dd.MM HH:mm");
        for (int i = all.size() - 1; i >= Math.max(0, all.size() - count); i--) {
            Session s = all.get(i);
            String type = s.type == null ? Tracker.FARMING : s.type;
            StringBuilder line = new StringBuilder("§8" + fmt.format(new java.util.Date(s.start)) + " "
                    + Hud.title(type).replace("§l", "") + " §a" + s.mainCrop
                    + " §7" + Fmt.duration(s.durationMs(0)) + " §6" + Fmt.coins(s.profit)
                    + " §7(§6" + Fmt.coins(s.profitPerHour) + "/h§7)");
            if (s.powder != null) for (var p : s.powder.entrySet())
                line.append(" §b+").append(Fmt.coins(p.getValue())).append(" ").append(p.getKey());
            Tracker.say(line.toString());
            if (s.rareDrops != null && !s.rareDrops.isEmpty()) {
                StringBuilder r = new StringBuilder("   §dRare: ");
                int n = 0;
                for (var d : s.rareDrops.entrySet()) {
                    if (n++ > 0) r.append("§7, §d");
                    r.append(d.getValue()).append("x ").append(d.getKey());
                }
                Tracker.say(r.toString());
            }
            if (s.pests != null && !s.pests.isEmpty()) Tracker.say("   §cPests killed: " + s.totalPests());
            if (s.treeGifts > 0) Tracker.say("   §aTree gifts: " + s.treeGifts);
            if (s.shards != null && !s.shards.isEmpty()) {
                StringBuilder r = new StringBuilder("   §bShards: ");
                int n = 0;
                for (var e : s.shards.entrySet()) {
                    if (n++ > 0) r.append("§7, §b");
                    r.append(e.getValue()).append("x ").append(e.getKey());
                }
                Tracker.say(r.toString());
            }
            if (s.fortune != null) Tracker.say("   §8" + s.fortune);
        }
        Tracker.say("§8Full details: .minecraft/config/farmprofit/history.json");
    }
}
