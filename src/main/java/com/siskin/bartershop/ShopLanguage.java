
package com.siskin.bartershop;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ShopLanguage {

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("bartershop.json");

    private static final Gson GSON = new Gson();

    private static final Map<String, String> ENGLISH = new HashMap<>();
    private static final Map<String, String> ACTIVE = new HashMap<>();

    private ShopLanguage() {
    }

    public static void initialize() {
        String language = "en_us";

        try {
            if (Files.notExists(CONFIG_PATH)) {
                JsonObject config = new JsonObject();
                config.addProperty("language", language);

                try (Writer writer = Files.newBufferedWriter(
                        CONFIG_PATH, StandardCharsets.UTF_8)) {
                    GSON.toJson(config, writer);
                }
            } else {
                try (Reader reader = Files.newBufferedReader(
                        CONFIG_PATH, StandardCharsets.UTF_8)) {
                    JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();

                    if (config.has("language")) {
                        language = config.get("language").getAsString();
                    }
                }
            }
        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            System.err.println("[BarterShop] Could not read language config: " + e.getMessage());
        }

        ENGLISH.clear();
        ACTIVE.clear();

        ENGLISH.putAll(loadLanguage("en_us"));
        ACTIVE.putAll(loadLanguage(language));
    }

    private static Map<String, String> loadLanguage(String language) {
        Map<String, String> translations = new HashMap<>();

        String resourcePath = "/assets/bartershop/lang/" + language + ".json";

        try (var stream = ShopLanguage.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                System.err.println("[BarterShop] Language not found: " + language);
                return translations;
            }

            try (Reader reader = new java.io.InputStreamReader(
                    stream, StandardCharsets.UTF_8)) {

                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                for (var entry : json.entrySet()) {
                    translations.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (IOException | IllegalStateException e) {
            System.err.println("[BarterShop] Could not load language: " + language);
        }

        return translations;
    }

    public static String translate(String key) {
        return ACTIVE.getOrDefault(
                key,
                ENGLISH.getOrDefault(key, key)
        );
    }
}
