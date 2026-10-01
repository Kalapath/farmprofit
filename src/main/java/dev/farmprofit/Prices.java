package dev.farmprofit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Pulls Bazaar prices from Hypixel's public API (no API key needed). */
public final class Prices {
    private static final String URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final long REFRESH_MS = 10 * 60 * 1000;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Map<String, double[]> PRICES = new ConcurrentHashMap<>(); // {instasell, sellorder}
    private static volatile long lastFetch;
    private static volatile boolean fetching;

    public static void tick() {
        if (!fetching && System.currentTimeMillis() - lastFetch > REFRESH_MS) refresh();
    }

    public static void refresh() {
        fetching = true;
        lastFetch = System.currentTimeMillis();
        HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
                .header("User-Agent", "FarmProfitCounter/1.0")
                .timeout(Duration.ofSeconds(20)).GET().build();
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> parse(res.body()))
                .whenComplete((ok, err) -> {
                    if (err != null) FarmProfitClient.LOG.warn("Bazaar fetch failed", err);
                    fetching = false;
                });
    }

    private static void parse(String body) {
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        if (!root.has("success") || !root.get("success").getAsBoolean()) return;
        for (var e : root.getAsJsonObject("products").entrySet()) {
            JsonObject qs = e.getValue().getAsJsonObject().getAsJsonObject("quick_status");
            if (qs == null) continue;
            PRICES.put(e.getKey(), new double[]{qs.get("sellPrice").getAsDouble(), qs.get("buyPrice").getAsDouble()});
        }
    }

    public static boolean loaded() { return !PRICES.isEmpty(); }

    public static double price(String itemName) {
        String id = Items.idFor(itemName);
        if (id == null) return 0;
        double[] p = PRICES.get(id);
        if (p == null) return 0;
        return "sellorder".equalsIgnoreCase(Config.get().priceMode) ? p[1] : p[0];
    }

    private Prices() {}
}
