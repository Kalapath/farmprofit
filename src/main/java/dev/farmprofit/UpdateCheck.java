package dev.farmprofit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Compares this jar's commit with the newest successful build in your GitHub repo. */
public final class UpdateCheck {
    private static boolean started;
    static volatile String message, link;

    public static String thisCommit() {
        try (InputStream in = UpdateCheck.class.getResourceAsStream("/farmprofit-build.txt")) {
            if (in == null) return "dev";
            String s = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            return s.isEmpty() || s.contains("$") ? "dev" : s;
        } catch (Exception e) { return "dev"; }
    }

    public static void start() {
        Config c = Config.get();
        if (started || !c.updateCheck || c.updateRepo == null || !c.updateRepo.contains("/")) return;
        started = true;
        String mine = thisCommit();
        if (mine.equals("dev")) return;
        String url = "https://api.github.com/repos/" + c.updateRepo.trim() + "/actions/runs?status=success&per_page=1";
        HttpClient.newHttpClient().sendAsync(HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "SkyBlockProfitCounter")
                        .header("Accept", "application/vnd.github+json").timeout(Duration.ofSeconds(20)).GET().build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> {
                    if (r.statusCode() != 200) return;
                    JsonObject run = JsonParser.parseString(r.body()).getAsJsonObject().getAsJsonArray("workflow_runs").get(0).getAsJsonObject();
                    String sha = run.get("head_sha").getAsString();
                    if (!sha.equals(mine)) {
                        link = run.get("html_url").getAsString();
                        message = "§6[Profit] §7A newer build of this mod is ready on GitHub. ";
                    }
                })
                .exceptionally(e -> null);
    }

    /** Shows the result on the game thread. */
    static void tick() {
        if (message == null) return;
        Tracker.say(Chat.link(message + "§a§n[Open download page]", link));
        message = null;
    }

    private UpdateCheck() {}
}
