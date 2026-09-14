/*
 * This is the source code of Impergram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Gleb Obitotsky <gleb.obitotsky@gmail.com>, 2026.
 */

package com.pqcs.impergram.music;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

final class MusicArtworkProvider {

    private static final String SEARCH_URL = "https://itunes.apple.com/search";
    private static final int CONNECT_TIMEOUT_MS = 7000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final String USER_AGENT = "Impergram/12.10.0";

    private MusicArtworkProvider() {}

    @Nullable
    static String findAndDownload(@NonNull String title, @Nullable String artist,
                                  @NonNull MusicArtworkCache cache, @NonNull String cacheKey) {
        String url = findArtworkUrl(title, artist);
        if (url == null) return null;
        return downloadToCache(url, cache, cacheKey);
    }

    @Nullable
    static String findArtworkUrl(@NonNull String title, @Nullable String artist) {
        try {
            final String term = (artist == null || artist.isEmpty())
                    ? title
                    : artist + " " + title;
            final String url = SEARCH_URL
                    + "?term=" + URLEncoder.encode(term, "UTF-8")
                    + "&entity=song&limit=5";
            final String json = httpGetString(url);
            if (json == null) return null;

            final JSONObject root = new JSONObject(json);
            final JSONArray results = root.optJSONArray("results");
            if (results == null || results.length() == 0) return null;

            String bestArtwork = null;
            int bestScore = -1;
            final String lt = title.toLowerCase(Locale.ROOT);
            final String la = artist == null ? "" : artist.toLowerCase(Locale.ROOT);

            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.optJSONObject(i);
                if (item == null) continue;
                String artistName = item.optString("artistName", "").toLowerCase(Locale.ROOT);
                String trackName  = item.optString("trackName",  "").toLowerCase(Locale.ROOT);
                String artwork    = item.optString("artworkUrl100", "");
                if (artwork.isEmpty()) continue;

                int score = 0;
                if (!la.isEmpty() && artistName.contains(la)) score += 2;
                if (trackName.contains(lt)) score += 2;
                if (score > bestScore) {
                    bestScore = score;
                    bestArtwork = artwork;
                }
            }
            if (bestArtwork == null) return null;

            return bestArtwork
                    .replace("100x100bb.jpg", "600x600bb.jpg")
                    .replace("100x100bb.png", "600x600bb.png")
                    .replace("100x100.jpg",   "600x600.jpg")
                    .replace("100x100.png",   "600x600.png");
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        }
    }

    @Nullable
    private static String downloadToCache(@NonNull String urlStr,
                                          @NonNull MusicArtworkCache cache,
                                          @NonNull String key) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", USER_AGENT);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

            try (InputStream in = conn.getInputStream()) {
                return cache.put(key, in);
            }
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @Nullable
    private static String httpGetString(@NonNull String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", USER_AGENT);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                return bos.toString("UTF-8");
            }
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}