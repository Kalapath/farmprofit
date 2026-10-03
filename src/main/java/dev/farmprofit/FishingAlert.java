package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Pings and shows "REEL IN!" the moment Hypixel's "!!!" appears over your bobber. */
public final class FishingAlert {
    private static long shownAt;
    private static boolean wasBiting;

    public static void tick(Minecraft mc) {
        if (!Config.get().fishingAlert || mc.player == null || mc.level == null) return;
        Object hook = Reflect.field(mc.player, "fishing");
        if (!(hook instanceof Entity bobber)) { wasBiting = false; return; }
        boolean biting = false;
        Object all = Reflect.call(mc.level, new String[]{"entitiesForRendering", "getEntities"});
        if (all instanceof Iterable<?> it) for (Object o : it) {
            if (!(o instanceof Entity e) || !e.hasCustomName() || e.getCustomName() == null) continue;
            if (Math.abs(e.getX() - bobber.getX()) > 1.5 || Math.abs(e.getZ() - bobber.getZ()) > 1.5 || Math.abs(e.getY() - bobber.getY()) > 3) continue;
            if (Tracker.strip(e.getCustomName().getString()).contains("!!!")) { biting = true; break; }
        }
        if (biting && !wasBiting) { shownAt = System.currentTimeMillis(); Chat.ping(); }
        wasBiting = biting;
    }

    public static void addHudLines(Hud.Lines out) {
        if (Config.get().fishingAlert && System.currentTimeMillis() - shownAt < 1500) out.add("§c§l>>> REEL IN! <<<");
    }

    private FishingAlert() {}
}
