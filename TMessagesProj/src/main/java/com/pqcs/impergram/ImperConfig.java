package com.pqcs.impergram;

import android.app.Activity;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.telegram.messenger.ApplicationLoader;

import java.util.HashSet;
import java.util.Set;

public class ImperConfig {
    public static boolean customNoiseSuppressor = false;

    private static final SharedPreferences preferences =
            ApplicationLoader.applicationContext.getSharedPreferences("imperconfig", Activity.MODE_PRIVATE);

    private static boolean configLoaded;

    static {
        loadConfig(false);
    }

    public static void loadConfig(boolean force) {
        if (configLoaded && !force) {
            return;
        }
        customNoiseSuppressor = preferences.getBoolean("customNoiseSuppressor", false);
        configLoaded = true;
    }

    public static void toggleCustomNoiseSuppressor() {
        customNoiseSuppressor = !customNoiseSuppressor;
        preferences.edit().putBoolean("customNoiseSuppressor", customNoiseSuppressor).apply();
    }

    public static String exportConfigs() {
        var object = new JsonObject();
        if (preferences.contains("customNoiseSuppressor")) {
            object.addProperty("customNoiseSuppressor", preferences.getBoolean("customNoiseSuppressor", false));
        }
        return object.toString();
    }

    public static void importConfigs(String config) {
        var map = JsonParser.parseString(config);
        if (!map.isJsonObject()) {
            throw new IllegalStateException("INVALID_BACKUP");
        }
        var object = map.getAsJsonObject();
        var editor = preferences.edit();
        editor.clear();
        if (object.has("customNoiseSuppressor")) {
            editor.putBoolean("customNoiseSuppressor", object.get("customNoiseSuppressor").getAsBoolean());
        }
        editor.apply();
        loadConfig(true);
    }
}