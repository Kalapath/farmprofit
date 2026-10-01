package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dungeon secret finder. No room database: it scans around you for things that are usually secrets
 * (chests, levers, Wither Essence skulls, secret items on the floor, bats) and points you to them,
 * and reads Hypixel's own "3/7 Secrets" room counter from the action bar.
 */
public final class Secrets {
    private static final Pattern ROOM = Pattern.compile("(\\d+)/(\\d+) Secrets");
    private static final String[] SECRET_ITEMS = {"Decoy", "Inflatable Jerry", "Spirit Leap", "Training Weight",
            "Defuse Kit", "Trap", "Treasure Talisman", "Dungeon Chest Key", "Healing VIII", "Revive Stone",
            "Architect's First Draft", "Secret Dye", "Candycomb", "Superboom TNT"};
    private static final int RADIUS = 14, HEIGHT = 7;

    public record Spot(String kind, double x, double y, double z, String color) {}

    public static int roomFound = -1, roomTotal = -1;
    private static long roomSeen;
    private static final List<Spot> spots = new ArrayList<>();
    private static final Set<Long> used = new HashSet<>();
    private static int tick;

    /** Action bar text, e.g. "... 3/7 Secrets ...". */
    public static void onActionBar(String text) {
        Matcher m = ROOM.matcher(text);
        if (m.find()) {
            roomFound = Integer.parseInt(m.group(1));
            roomTotal = Integer.parseInt(m.group(2));
            roomSeen = System.currentTimeMillis();
        }
    }

    /** You right-clicked a block (opened a chest, flicked a lever, picked an essence). */
    public static void onUse(BlockPos pos) { used.add(pos.asLong()); }

    public static void tick(Minecraft mc) {
        if (!Tracker.DUNGEONS.equals(Tracker.area) || mc.player == null || mc.level == null) {
            if (!spots.isEmpty() || !used.isEmpty()) { spots.clear(); used.clear(); roomTotal = -1; }
            return;
        }
        if (!Config.get().secretFinder || ++tick % 10 != 0) return;   // twice a second

        List<Spot> found = new ArrayList<>();
        BlockPos me = mc.player.blockPosition();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
                    BlockPos p = new BlockPos(me.getX() + dx, me.getY() + dy, me.getZ() + dz);
                    BlockState s = mc.level.getBlockState(p);
                    String kind = null, color = "§e";
                    if (s.is(Blocks.CHEST) || s.is(Blocks.TRAPPED_CHEST)) kind = "Chest";
                    else if (s.is(Blocks.LEVER)) { kind = "Lever"; color = "§6"; }
                    else if (s.is(Blocks.PLAYER_HEAD) || s.is(Blocks.PLAYER_WALL_HEAD)) { kind = "Essence"; color = "§d"; }
                    if (kind != null && !used.contains(p.asLong())) found.add(new Spot(kind, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, color));
                }
            }
        }
        Object entities = Reflect.call(mc.level, new String[]{"entitiesForRendering", "getEntities"});
        if (entities instanceof Iterable<?> it) {
            for (Object o : it) {
                if (!(o instanceof Entity e) || e == mc.player) continue;
                if (Math.abs(e.getX() - mc.player.getX()) > 24 || Math.abs(e.getZ() - mc.player.getZ()) > 24) continue;
                String type = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
                if (type.equals("bat") && !e.isInvisible()) found.add(new Spot("Bat", e.getX(), e.getY(), e.getZ(), "§c"));
                else if (type.equals("item")) {
                    Object stack = Reflect.call(e, "getItem");
                    if (stack instanceof ItemStack is) {
                        String name = Tracker.strip(is.getHoverName().getString());
                        for (String s : SECRET_ITEMS) if (name.contains(s)) {
                            found.add(new Spot(name, e.getX(), e.getY(), e.getZ(), "§b"));
                            break;
                        }
                    }
                }
            }
        }
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        found.sort((a, b) -> Double.compare(dist2(a, px, py, pz), dist2(b, px, py, pz)));
        spots.clear();
        spots.addAll(found);
    }

    private static double dist2(Spot s, double x, double y, double z) {
        return (s.x - x) * (s.x - x) + (s.y - y) * (s.y - y) + (s.z - z) * (s.z - z);
    }

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    /** Arrow pointing to the spot, relative to where you're looking. */
    private static String arrow(Spot s, Minecraft mc) {
        double dx = s.x - mc.player.getX(), dz = s.z - mc.player.getZ();
        double target = Math.toDegrees(Math.atan2(-dx, dz));           // Minecraft yaw convention
        double rel = ((target - mc.player.getYRot()) % 360 + 540) % 360 - 180;   // -180..180, 0 = straight ahead
        int idx = (int) Math.round(rel / 45.0);
        return ARROWS[((idx % 8) + 8) % 8];
    }

    public static void addHudLines(Hud.Lines out) {
        if (!Tracker.DUNGEONS.equals(Tracker.area) || !Config.get().secretFinder) return;
        Minecraft mc = Minecraft.getInstance();
        boolean fresh = System.currentTimeMillis() - roomSeen < 5000 && roomTotal >= 0;
        String room = fresh ? (roomFound >= roomTotal ? " §a✔ room done" : " §7room §f" + roomFound + "/" + roomTotal
                + " §8(" + (roomTotal - roomFound) + " left)") : "";
        out.add("§5§lSecrets" + room);
        if (spots.isEmpty()) { out.add(" §8nothing nearby"); return; }
        for (int i = 0; i < Math.min(5, spots.size()); i++) {
            Spot s = spots.get(i);
            double dy = s.y - mc.player.getY();
            String level = dy > 2.5 ? " §7▲" : dy < -2.5 ? " §7▼" : "";
            double d = Math.sqrt(dist2(s, mc.player.getX(), mc.player.getY(), mc.player.getZ()));
            out.add(" §f" + arrow(s, mc) + " " + s.color + s.kind + " §7" + String.format(Locale.US, "%.0fm", d) + level);
        }
    }

    private Secrets() {}
}
