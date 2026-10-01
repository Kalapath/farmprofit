package dev.farmprofit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Garden visitors. The items they ask for are counted as "Spent" (taken while their menu is open,
 * or from sacks), item rewards count as profit, copper is valued with the copperValue setting.
 */
public final class Visitors {
    private static final Pattern ACCEPTED = Pattern.compile("^OFFER ACCEPTED with (.+?)(?: \\((.+)\\))?$");
    private static final Pattern REWARD = Pattern.compile("^\\+?([\\d,.]+[kKmM]?)x? (.+)$");
    private static long rewardWindowUntil;

    /** Returns true if the message was part of a visitor offer. */
    public static boolean onChat(String plain) {
        Matcher m = ACCEPTED.matcher(plain);
        if (m.find()) {
            Session s = Tracker.farmingSession();
            s.visitors++;
            rewardWindowUntil = System.currentTimeMillis() + 3000;
            Debug.saw("visitor");
            return true;
        }
        if (System.currentTimeMillis() > rewardWindowUntil) return false;
        String line = plain.trim();
        if (line.equals("REWARDS")) return true;
        Matcher r = REWARD.matcher(line);
        if (r.matches()) {
            String what = r.group(2).trim();
            double amount = FlipsCommand.parseAmount(r.group(1));
            Session s = Tracker.farmingSession();
            if (what.equalsIgnoreCase("Copper")) {
                s.copper += (long) amount;
                return true;
            }
            if (what.contains("XP") || what.contains("Experience") || what.contains("Bits")) return true;
            return false;   // item rewards land in your inventory/sacks and are counted there
        }
        return false;
    }

    private Visitors() {}
}
