package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kills, mob names and "grinds" (farming one special mob, like Zealots).
 * A kill = a mob you hit that dies or disappears within 3 seconds.
 */
public final class Combat {
    /** A mob people farm for a rare drop: which names count, and which drops to track odds for. */
    public record Grind(String name, List<String> mobs, List<String> drops, String specialMessage, String specialName) {}

    public static final List<Grind> GRINDS = List.of(
            new Grind("Zealot", List.of("Zealot", "Zealot Bruiser", "Special Zealot"), List.of("Summoning Eye", "Enchanted Ender Pearl"),
                    "A special Zealot has spawned nearby", "Special Zealots"),
            new Grind("Enderman", List.of("Enderman", "Voidling Fanatic", "Voidling Extremist"), List.of("Ender Pearl", "Enchanted Ender Pearl", "Null Sphere"), null, null),
            new Grind("Ghost", List.of("Ghost"), List.of("Sorrow", "Plasma", "Volta", "Ghostly Boots", "Bag of Cash"), null, null),
            new Grind("Blaze", List.of("Blaze", "Bezal", "Mutated Blaze", "Smoldering Blaze"), List.of("Blaze Rod", "Enchanted Blaze Powder"), null, null),
            new Grind("Wither Skeleton", List.of("Wither Skeleton", "Wither Spectre"), List.of("Wither Skeleton Skull", "Enchanted Coal"), null, null),
            new Grind("Magma Cube", List.of("Magma Cube", "Magma Cube Rider"), List.of("Magma Cream", "Enchanted Magma Cream"), null, null),
            new Grind("Ice Walker", List.of("Ice Walker", "Glacite Walker"), List.of("Glacite Jewel", "Enchanted Glacite"), null, null),
            new Grind("Spider", List.of("Arachne's Keeper", "Arachne's Brood", "Splitter Spider"), List.of("Spider Eye", "Enchanted Spider Eye"), null, null)
    );

    private static final Pattern NAME = Pattern.compile("^(?:\\[Lv[\\d,]+\\]\\s*)?(?:[^A-Za-z\\[]+\\s)?(.+?)\\s+[\\d.,]+[kKmMbB]?(?:/[\\d.,]+[kKmMbB]?)?\\s*[❤♥]");
    private static final long KILL_WINDOW = 3000;

    private record Hit(long time, String name, String target) {}
    private static final Map<Integer, Hit> hits = new HashMap<>();

    /** Which grind a mob name belongs to, or null. */
    public static Grind grindFor(String mob) {
        if (mob == null) return null;
        for (Grind g : GRINDS) for (String m : g.mobs()) if (mob.equalsIgnoreCase(m) || mob.endsWith(" " + m)) return g;
        return null;
    }

    public static Grind byName(String name) {
        for (Grind g : GRINDS) if (g.name().equals(name)) return g;
        return null;
    }

    /** Hypixel shows mob names on a floating nametag ("[Lv55] Zealot 13k❤"); read the one above this mob. */
    public static String nameOf(Entity mob) {
        Minecraft mc = Minecraft.getInstance();
        try {
            var box = mob.getBoundingBox().inflate(0.6, 3.0, 0.6);
            for (Entity e : mc.level.getEntities(mob, box)) {
                if (!e.hasCustomName() || e.getCustomName() == null) continue;
                Matcher m = NAME.matcher(Tracker.strip(e.getCustomName().getString()).trim());
                if (m.find()) return m.group(1).trim();
            }
            if (mob.hasCustomName() && mob.getCustomName() != null) {
                Matcher m = NAME.matcher(Tracker.strip(mob.getCustomName().getString()).trim());
                if (m.find()) return m.group(1).trim();
            }
        } catch (Throwable ignored) {}
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath().replace('_', ' ');
        return Character.toUpperCase(type.charAt(0)) + type.substring(1);
    }

    /** Remember the hit so we can count the kill. target = which session gets it. */
    public static void hit(Entity mob, String name, String target) {
        hits.put(mob.getId(), new Hit(System.currentTimeMillis(), name, target));
    }

    /** Every tick: mobs we hit recently that are now dead or gone count as kills. */
    public static void tick(Minecraft mc) {
        if (mc.level == null || hits.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<Integer, Hit>> it = hits.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Hit h = e.getValue();
            Entity mob = mc.level.getEntity(e.getKey());
            boolean dead = mob == null || !mob.isAlive() || mob.isRemoved();
            if (dead && now - h.time() <= KILL_WINDOW) {
                Tracker.onKill(h.name(), h.target());
                it.remove();
            } else if (now - h.time() > KILL_WINDOW) {
                it.remove();
            }
        }
    }

    /** "1 per 340 kills" style odds for a drop in this session. */
    public static String odds(int kills, int drops) {
        if (drops <= 0) return kills > 0 ? "§8none in " + Fmt.num(kills) + " kills" : "";
        return "§81 per " + Fmt.num(Math.round((double) kills / drops)) + " kills";
    }

    public static String lower(String s) { return s.toLowerCase(Locale.ROOT); }

    private Combat() {}
}
