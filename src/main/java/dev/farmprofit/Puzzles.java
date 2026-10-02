package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Dungeon puzzles that can be solved from chat and nametags: Three Weirdos and Blaze (Higher or Lower). */
public final class Puzzles {
    // Three Weirdos: the one who says one of these lines has the reward.
    private static final Pattern NPC = Pattern.compile("^\\[NPC\\] (\\w+): (.+)$");
    private static final List<Pattern> WEIRDO_TRUTHS = List.of(
            Pattern.compile("The reward is not in my chest!?"),
            Pattern.compile("At least one of them's lying, and the reward is not in \\w+'s chest[.!]?"),
            Pattern.compile("My chest doesn't have the reward\\. We are all telling the truth\\.?"),
            Pattern.compile("My chest has the reward and I'm telling the truth!?"),
            Pattern.compile("The reward isn't in any of our chests\\.?"),
            Pattern.compile("Both of them are telling the truth\\. Also, \\w+ has the reward in their chest!?"));
    private static final Pattern BLAZE = Pattern.compile("Blaze.*?([\\d,]+)/([\\d,]+)\\s*[❤♥]");

    /** Returns true if the chat line was a Three Weirdos answer. */
    public static boolean onChat(String plain) {
        if (!Config.get().solveWeirdos || !Tracker.DUNGEONS.equals(Tracker.area)) return false;
        Matcher m = NPC.matcher(plain);
        if (!m.matches()) return false;
        for (Pattern p : WEIRDO_TRUTHS) {
            if (p.matcher(m.group(2).trim()).matches()) {
                Chat.ping();
                Tracker.say("§a§l[Puzzle] §fOpen §e§l" + m.group(1) + "§f's chest!");
                Debug.saw("three weirdos");
                return true;
            }
        }
        return false;
    }

    private record Blaze(double x, double y, double z, long hp) {}

    /** Blaze puzzle: the blazes with the lowest and highest health, with arrows (kill order depends on the room). */
    public static void addBlazeLines(Hud.Lines out) {
        if (!Config.get().solveBlaze || !Tracker.DUNGEONS.equals(Tracker.area)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        List<Blaze> blazes = new ArrayList<>();
        Object all = Reflect.call(mc.level, new String[]{"entitiesForRendering", "getEntities"});
        if (!(all instanceof Iterable<?> it)) return;
        for (Object o : it) {
            if (!(o instanceof Entity e) || !e.hasCustomName() || e.getCustomName() == null) continue;
            if (e.distanceTo(mc.player) > 40) continue;
            Matcher m = BLAZE.matcher(Tracker.strip(e.getCustomName().getString()));
            if (m.find()) blazes.add(new Blaze(e.getX(), e.getY(), e.getZ(), Long.parseLong(m.group(1).replace(",", ""))));
        }
        if (blazes.size() < 3) return;                       // not the puzzle room
        blazes.sort((a, b) -> Long.compare(a.hp(), b.hp()));
        Blaze low = blazes.get(0), high = blazes.get(blazes.size() - 1);
        out.add("§6§lBlaze puzzle §7" + blazes.size() + " left");
        out.add(" §fLowest: §a" + Fmt.num(low.hp()) + " HP " + where(low, mc));
        out.add(" §fHighest: §c" + Fmt.num(high.hp()) + " HP " + where(high, mc));
        out.add(" §8Chest at the top: lowest first. At the bottom: highest first.");
    }

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private static String where(Blaze b, Minecraft mc) {
        double dx = b.x() - mc.player.getX(), dz = b.z() - mc.player.getZ(), dy = b.y() - mc.player.getY();
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = ((target - mc.player.getYRot()) % 360 + 540) % 360 - 180;
        int idx = (int) Math.round(rel / 45.0);
        String arrow = ARROWS[((idx % 8) + 8) % 8];
        return "§f" + arrow + " §7" + String.format(Locale.US, "%.0fm", Math.sqrt(dx * dx + dy * dy + dz * dz))
                + (dy > 2.5 ? " ▲" : dy < -2.5 ? " ▼" : "");
    }

    private Puzzles() {}
}
