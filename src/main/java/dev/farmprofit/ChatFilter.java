package dev.farmprofit;

import java.util.Locale;

/** Hides chat spam you don't need (the mod still reads it first, so tracking keeps working). */
public final class ChatFilter {
    public static boolean allow(String plain) {
        Config c = Config.get();
        if (c.chatHideSacks && plain.startsWith("[Sacks]")) return false;
        if (c.chatHideCooldown && (plain.contains("This ability is on cooldown") || plain.contains("ability is currently on cooldown"))) return false;
        if (c.chatHideBlocksInWay && plain.contains("There are blocks in the way")) return false;
        if (c.chatHideVisitorChat && Tracker.FARMING.equals(Tracker.area) && plain.startsWith("[NPC]")) return false;
        if (c.chatHideWatchdog && (plain.contains("[WATCHDOG ANNOUNCEMENT]") || plain.startsWith("Watchdog has banned") || plain.startsWith("Staff have banned")
                || plain.startsWith("Blacklisted modifications are a bannable") || plain.startsWith("Watchdog has banned"))) return false;
        String low = plain.toLowerCase(Locale.ROOT);
        for (String f : c.chatHideContaining) if (!f.isBlank() && low.contains(f.trim().toLowerCase(Locale.ROOT))) return false;
        return true;
    }

    private ChatFilter() {}
}
