/*
 * This is the source code of Impergram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Gleb Obitotsky <gleb.obitotsky@gmail.com>, 2026.
 */

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
    public static boolean blurPhone = false;
    public static boolean blurID = false;
    public static boolean blurUsername = false;

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
        blurPhone = preferences.getBoolean("blurPhone", false);
        blurID = preferences.getBoolean("blurID", false);
        blurUsername = preferences.getBoolean("blurUsername", false);
        configLoaded = true;
    }

    public static void toggleCustomNoiseSuppressor() {
        customNoiseSuppressor = !customNoiseSuppressor;
        preferences.edit().putBoolean("customNoiseSuppressor", customNoiseSuppressor).apply();
    }

    public static void toggleBlurPhone() {
        blurPhone = !blurPhone;
        preferences.edit().putBoolean("blurPhone", blurPhone).apply();
    }

    public static void toggleBlurID() {
        blurID = !blurID;
        preferences.edit().putBoolean("blurID", blurID).apply();
    }

    public static void toggleBlurUsername() {
        blurUsername = !blurUsername;
        preferences.edit().putBoolean("blurUsername", blurUsername).apply();
    }

    public static String exportConfigs() {
        var object = new JsonObject();
        if (preferences.contains("customNoiseSuppressor")) {
            object.addProperty("customNoiseSuppressor", preferences.getBoolean("customNoiseSuppressor", false));
        }
        if (preferences.contains("blurPhone")) {
            object.addProperty("blurPhone", preferences.getBoolean("blurPhone", false));
        }
        if (preferences.contains("blurID")) {
            object.addProperty("blurID", preferences.getBoolean("blurID", false));
        }
        if (preferences.contains("blurUsername")) {
            object.addProperty("blurUsername", preferences.getBoolean("blurUsername", false));
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
        if (object.has("blurPhone")) {
            editor.putBoolean("blurPhone", object.get("blurPhone").getAsBoolean());
        }
        if (object.has("blurID")) {
            editor.putBoolean("blurID", object.get("blurID").getAsBoolean());
        }
        if (object.has("blurUsername")) {
            editor.putBoolean("blurUsername", object.get("blurUsername").getAsBoolean());
        }
        editor.apply();
        loadConfig(true);
    }
}