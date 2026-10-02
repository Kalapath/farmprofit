package dev.farmprofit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Buy the ingredients, craft, sell for more:
 *  - to the Bazaar (e.g. Enchanted Diamond -> Enchanted Diamond Block), with volume so you know it sells,
 *  - to the Auction House (armor, weapons, tools...), priced at lowest BIN.
 */
public final class CraftFlips {
    public record Flip(String id, String name, double cost, double sell, double profit, double margin, double perHour, int makes,
                       String ingredients, String note) {}

    private static double buyNow(String id) {
        double[] bz = Prices.bazaarRaw(id);
        if (bz == null && id.contains("-")) bz = Prices.bazaarRaw(id.replace('-', ':'));
        if (bz != null && bz[1] > 0) return bz[1];
        double bin = Prices.binPrice(id);
        return bin > 0 ? bin : 0;
    }

    /** toAuction=false: result sold on the Bazaar. true: result sold on the Auction House. */
    public static List<Flip> compute(boolean toAuction) {
        Config c = Config.get();
        double tax = c.bzTax / 100.0;
        List<Flip> out = new ArrayList<>();
        for (var e : CraftCost.recipes().entrySet()) {
            String id = e.getKey();
            Map<String, Integer> ing = e.getValue().getKey();
            int makes = Math.max(1, e.getValue().getValue());
            double[] book = Prices.BOOK.get(id);
            boolean onBazaar = book != null && book[1] > 0;
            if (toAuction == onBazaar) continue;
            double cost = 0;
            StringBuilder list = new StringBuilder();
            boolean ok = true;
            for (var i : ing.entrySet()) {
                double each = buyNow(i.getKey());
                if (each <= 0) { ok = false; break; }
                cost += each * i.getValue();
                list.append(i.getValue()).append("x ").append(Prices.nameOf(i.getKey().replace('-', ':'))).append(", ");
            }
            if (!ok || cost <= 0) continue;
            double sell, perHour;
            String note = null;
            if (!toAuction) {
                double offer = book[1] - 0.1;                       // sell offer just under the lowest one
                sell = offer * (1 - tax) * makes;
                double weekly = Math.min(book[2], book[3]);
                if (weekly < c.bzMinVolume) continue;
                double craftsPerHour = Math.min(weekly / 168.0 * c.bzShare / 100.0 / makes, c.bzBudget / cost);
                double profit = sell - cost;
                perHour = Math.max(0, profit) * craftsPerHour;
                if (weekly / 168 < 50) note = "slow to sell";
            } else {
                double bin = Prices.binPrice(id);
                if (bin <= 0) continue;
                double ahTax = bin >= 100_000_000 ? 0.035 : bin >= 10_000_000 ? 0.03 : 0.02;   // listing fee + claim tax (approx.)
                sell = bin * (1 - ahTax) * makes;
                perHour = 0;
                note = "AH demand unknown — check recent sales";
                if (cost > c.bzBudget) continue;
            }
            double profit = sell - cost;
            if (profit <= c.craftFlipMinProfit) continue;
            double margin = profit / cost * 100;
            if (margin < c.bzMinMargin || margin > 500) continue;   // >500% is almost always bad data
            String ingredients = list.length() > 2 ? list.substring(0, list.length() - 2) : "";
            out.add(new Flip(id, Prices.nameOf(id), cost, sell, profit, margin, perHour, makes, ingredients, note));
        }
        if (!toAuction) out.sort((a, b) -> Double.compare(b.perHour(), a.perHour()));
        else out.sort((a, b) -> Double.compare(b.profit(), a.profit()));
        return out;
    }

    private CraftFlips() {}
}
