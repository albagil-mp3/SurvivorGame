package killergame;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/**
 * Simple JSON-backed highscore store.
 * File format: {"highscore":123}
 */
public class HighscoreStore {

    private static final Path HS_FILE = Path.of("highscore.json");

    public static int getHighscore() {
        try {
            if (!Files.exists(HS_FILE)) return 0;
            String s = Files.readString(HS_FILE, StandardCharsets.UTF_8).trim();
            if (s.isEmpty()) return 0;
            // crude parse for {"highscore":NUMBER}
            int idx = s.indexOf("highscore");
            if (idx < 0) return 0;
            int colon = s.indexOf(':', idx);
            if (colon < 0) return 0;
            String num = s.substring(colon + 1).replaceAll("[^0-9-]", "").trim();
            if (num.isEmpty()) return 0;
            return Integer.parseInt(num);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void saveHighscore(int score) {
        try {
            String json = "{\"highscore\":" + score + "}";
            Files.writeString(HS_FILE, json, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            // ignore write errors silently
        }
    }
}
