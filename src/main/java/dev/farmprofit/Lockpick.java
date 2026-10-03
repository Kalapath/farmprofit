package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Locale;

/**
 * Treasure chest lockpick helper (display only, you still aim): Hypixel shows "crit" particles on the chest;
 * this marks the latest one with a bright pink dot and tells you on the HUD which way to move your aim.
 * Wiki tip built in: the hit spot is a pixel or two above the particles.
 */
public final class Lockpick {
    public static volatile boolean hooked;            // the particle hook is working
    private static volatile double tx, ty, tz;
    private static volatile long seenAt;
    private static int tick;

    /** Called from the mixin for every particle packet (on the game thread). */
    public static void onPacket(Object packet) {
        hooked = true;
        try {
            Object particle = Reflect.call(packet, new String[]{"getParticle", "particle"});
            Object type = Reflect.call(particle, "getType");
            if (!(type instanceof net.minecraft.core.particles.ParticleType<?> pt)) return;
            String key = BuiltInRegistries.PARTICLE_TYPE.getKey(pt).getPath();
            double x = Reflect.num(Reflect.call(packet, new String[]{"getX", "x"}), Double.NaN);
            double y = Reflect.num(Reflect.call(packet, new String[]{"getY", "y"}), Double.NaN);
            double z = Reflect.num(Reflect.call(packet, new String[]{"getZ", "z"}), Double.NaN);
            if (Double.isNaN(x)) return;
            DianaBurrows.onParticle(key, x, y, z);
            if (!Config.get().lockpickHelper || !Tracker.MINING.equals(Tracker.area)) return;
            if (!key.equals("crit")) return;
            if (!PowderChests.nearChest(x, y, z)) return;
            tx = x; ty = y + Config.get().lockpickOffset / 16.0; tz = z;            // a pixel or two above the particles
            seenAt = System.currentTimeMillis();
        } catch (Throwable ignored) {}
    }

    private static boolean active() { return System.currentTimeMillis() - seenAt < 1500; }

    public static void tick(Minecraft mc) {
        if (!active() || mc.level == null || ++tick % 2 != 0) return;
        var pink = Particles.dust(0xFF40FF, 0.7f);
        Particles.point(pink, tx, ty, tz);
        // a tiny cross around the spot so it stands out from the chest texture
        Particles.point(pink, tx + 0.06, ty, tz);
        Particles.point(pink, tx - 0.06, ty, tz);
        Particles.point(pink, tx, ty + 0.06, tz);
        Particles.point(pink, tx, ty - 0.06, tz);
    }

    /** HUD: where to move your aim (or "on target"). */
    public static void addHudLines(Hud.Lines out) {
        if (!Config.get().lockpickHelper) return;
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.player == null) return;
        double dx = tx - mc.player.getX(), dy = ty - mc.player.getEyeY(), dz = tz - mc.player.getZ();
        double yawTo = Math.toDegrees(Math.atan2(-dx, dz));
        double pitchTo = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        double dYaw = ((yawTo - mc.player.getYRot()) % 360 + 540) % 360 - 180;
        double dPitch = pitchTo - mc.player.getXRot();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double tol = Math.toDegrees(Math.atan2(0.07, Math.max(0.5, dist)));         // ~1 pixel of the chest
        if (Math.abs(dYaw) <= tol && Math.abs(dPitch) <= tol) { out.add("§d§lLockpick §a§l✔ ON TARGET"); return; }
        StringBuilder dir = new StringBuilder();
        if (dPitch < -tol) dir.append("↑ ");
        if (dPitch > tol) dir.append("↓ ");
        if (dYaw < -tol) dir.append("← ");
        if (dYaw > tol) dir.append("→ ");
        out.add("§d§lLockpick §faim at the pink dot §e" + dir.toString().trim()
                + String.format(Locale.US, " §8(%.1f°)", Math.max(Math.abs(dYaw), Math.abs(dPitch))));
    }

    private Lockpick() {}
}
