package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes pests easy to see in the Garden: a particle column and a trail toward each pest (only you see them),
 * and a list with arrows, distance and the plot numbers from the tab list.
 */
public final class Pests {
    private static final String[] NAMES = {"Beetle", "Cricket", "Fly", "Locust", "Mite", "Mosquito", "Moth", "Rat", "Slug",
            "Earthworm", "Mouse", "Field Mouse", "Dragonfly", "Firefly", "Mantis", "Praying Mantis"};
    private static final Pattern PEST_TAG = Pattern.compile("ൠ\\s*([A-Za-z ]+?)(?:\\s+[\\d.,]+[kKmM]?(?:/[\\d.,]+[kKmM]?)?\\s*❤)?\\s*$");
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    public record Pest(String name, double x, double y, double z) {}

    private static final List<Pest> pests = new ArrayList<>();
    private static int tick;

    public static void tick(Minecraft mc) {
        if (!Config.get().pestHighlight || mc.player == null || mc.level == null || !Tracker.FARMING.equals(Tracker.area)) {
            pests.clear();
            return;
        }
        if (++tick % 4 == 0) scan(mc);                      // 5x a second
        if (tick % 6 != 0) return;
        for (Pest p : pests) {
            Particles.pillar(Particles.FIRE, p.x(), p.y() + 0.6, p.z());
            if (Config.get().pestTrail) {
                // a short dotted trail from you toward the pest, so you can follow it
                double dx = p.x() - mc.player.getX(), dy = p.y() - mc.player.getY(), dz = p.z() - mc.player.getZ();
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (d > 2) {
                    double len = Math.min(6, d - 1);
                    Particles.line(Particles.FIRE, mc.player.getX(), mc.player.getY() + 1, mc.player.getZ(),
                            mc.player.getX() + dx / d * len, mc.player.getY() + 1 + dy / d * len, mc.player.getZ() + dz / d * len);
                }
            }
        }
    }

    private static void scan(Minecraft mc) {
        pests.clear();
        Object all = Reflect.call(mc.level, new String[]{"entitiesForRendering", "getEntities"});
        if (!(all instanceof Iterable<?> it)) return;
        for (Object o : it) {
            if (!(o instanceof Entity e) || !e.hasCustomName() || e.getCustomName() == null) continue;
            String tag = Tracker.strip(e.getCustomName().getString()).trim();
            String name = null;
            Matcher m = PEST_TAG.matcher(tag);
            if (tag.contains("ൠ") && m.find()) name = m.group(1).trim();
            if (name == null) continue;
            pests.add(new Pest(name, e.getX(), e.getY(), e.getZ()));
        }
        double px = mc.player.getX(), pz = mc.player.getZ();
        pests.sort((a, b) -> Double.compare(Math.hypot(a.x() - px, a.z() - pz), Math.hypot(b.x() - px, b.z() - pz)));
    }

    private static String arrow(Pest p, Minecraft mc) {
        double target = Math.toDegrees(Math.atan2(-(p.x() - mc.player.getX()), p.z() - mc.player.getZ()));
        double rel = ((target - mc.player.getYRot()) % 360 + 540) % 360 - 180;
        int idx = (int) Math.round(rel / 45.0);
        return ARROWS[((idx % 8) + 8) % 8];
    }

    /** HUD lines for the Farming HUD. */
    public static void addHudLines(Hud.Lines out) {
        if (!Config.get().pestHighlight || !Tracker.FARMING.equals(Tracker.area)) return;
        Minecraft mc = Minecraft.getInstance();
        String alive = Tracker.tab.get("Alive");
        String plots = Tracker.tab.get("Plots");
        if (pests.isEmpty() && (alive == null || alive.startsWith("0"))) return;
        out.add("§c§lൠ Pests" + (alive != null ? " §7" + alive + " alive" : "") + (plots != null ? " §8(plots " + plots + ")" : ""));
        for (int i = 0; i < Math.min(5, pests.size()); i++) {
            Pest p = pests.get(i);
            double dy = p.y() - mc.player.getY();
            double d = Math.sqrt(Math.pow(p.x() - mc.player.getX(), 2) + dy * dy + Math.pow(p.z() - mc.player.getZ(), 2));
            out.add(" §f" + arrow(p, mc) + " §c" + p.name() + " §7" + String.format(Locale.US, "%.0fm", d) + (dy > 3 ? " §7▲" : dy < -3 ? " §7▼" : ""));
        }
        if (pests.isEmpty()) out.add(" §8none nearby — check the plots above");
    }

    public static boolean isPestName(String n) {
        for (String s : NAMES) if (s.equalsIgnoreCase(n)) return true;
        return false;
    }

    private Pests() {}
}
