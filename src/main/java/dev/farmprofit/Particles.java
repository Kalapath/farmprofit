package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/** In-world markers made of client-side particles (only you see them). Used by the dungeon puzzle solvers. */
final class Particles {
    static final ParticleOptions GREEN = ParticleTypes.HAPPY_VILLAGER;
    static final ParticleOptions FIRE = ParticleTypes.FLAME;
    static final ParticleOptions WHITE = ParticleTypes.END_ROD;
    static final ParticleOptions BLUE = ParticleTypes.SOUL_FIRE_FLAME;
    static final ParticleOptions[] PAIRS = {GREEN, FIRE, WHITE, BLUE};

    static void point(ParticleOptions p, double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !Config.get().puzzleParticles) return;
        try { mc.level.addParticle(p, x, y, z, 0, 0, 0); } catch (Throwable ignored) {}
    }

    /** A dotted line between two points. */
    static void line(ParticleOptions p, double x1, double y1, double z1, double x2, double y2, double z2) {
        double len = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
        int steps = (int) Math.min(80, Math.max(2, len * 2));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            point(p, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t);
        }
    }

    /** A small column marking one block. */
    static void pillar(ParticleOptions p, double x, double y, double z) {
        for (int i = 0; i < 4; i++) point(p, x, y + i * 0.4, z);
    }

    private Particles() {}
}
