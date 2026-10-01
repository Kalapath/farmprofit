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

/** Bazaar prices from Hypixel's public API, plus lowest-BIN prices for auction items. */
public final class Prices {
    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final long REFRESH_MS = 10 * 60 * 1000;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Map<String, double[]> BAZAAR = new ConcurrentHashMap<>(); // {instasell, sellorder}
    private static final Map<String, Double> BINS = new ConcurrentHashMap<>();
    private static volatile long lastFetch;

    public static void tick() {
        if (System.currentTimeMillis() - lastFetch > REFRESH_MS) refresh();
    }

    public static void refresh() {
        lastFetch = System.currentTimeMillis();
        fetch(BAZAAR_URL, Prices::parseBazaar);
        String bin = Config.get().lowestBinUrl;
        if (bin != null && !bin.isBlank()) fetch(bin, Prices::parseBins);
    }

    private static void fetch(String url, java.util.function.Consumer<String> parser) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "SkyBlockProfitCounter/1.2")
                .timeout(Duration.ofSeconds(30)).GET().build();
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    if (res.statusCode() == 200) parser.accept(res.body());
                    else FarmProfitClient.LOG.warn("Price fetch {} returned {}", url, res.statusCode());
                })
                .exceptionally(err -> { FarmProfitClient.LOG.warn("Price fetch failed: {}", url, err); return null; });
    }

    private static void parseBazaar(String body) {
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        if (!root.has("success") || !root.get("success").getAsBoolean()) return;
        for (var e : root.getAsJsonObject("products").entrySet()) {
            JsonObject qs = e.getValue().getAsJsonObject().getAsJsonObject("quick_status");
            if (qs == null) continue;
            BAZAAR.put(e.getKey(), new double[]{qs.get("sellPrice").getAsDouble(), qs.get("buyPrice").getAsDouble()});
        }
    }

    private static void parseBins(String body) {
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        for (var e : root.entrySet()) {
            try { BINS.put(e.getKey(), e.getValue().getAsDouble()); } catch (Exception ignored) {}
        }
    }

    public static boolean loaded() { return !BAZAAR.isEmpty(); }

    public static double price(String itemName) {
        String id = Items.idFor(itemName);
        if (id == null) id = Items.guessId(itemName);
        double v = priceForId(id);
        if (v > 0 || !itemName.endsWith(" Shard")) return v;
        for (String cand : shardIds(itemName)) {
            v = priceForId(cand);
            if (v > 0) return v;
        }
        return 0;
    }

    private static double priceForId(String id) {
        double[] p = BAZAAR.get(id);
        if (p != null) return "sellorder".equalsIgnoreCase(Config.get().priceMode) ? p[1] : p[0];
        Double bin = BINS.get(id);
        return bin == null ? 0 : bin;
    }

    /** Possible Bazaar IDs for a shard, e.g. "Sparrow Shard" -> SHARD_SPARROW. */
    private static String[] shardIds(String shardName) {
        String base = Items.guessId(shardName.substring(0, shardName.length() - " Shard".length()));
        return new String[]{"SHARD_" + base, base + "_SHARD", "ATTRIBUTE_SHARD_" + base};
    }

    /** True if Bazaar/BIN data knows this shard (used to clean up names read from chat). */
    public static boolean knowsShard(String shardName) {
        for (String cand : shardIds(shardName)) if (BAZAAR.containsKey(cand) || BINS.containsKey(cand)) return true;
        return false;
    }

    private Prices() {}
}
