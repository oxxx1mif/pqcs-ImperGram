/*
 * This is the source code of Impergram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Gleb Obitotsky <gleb.obitotsky@gmail.com>, 2026.
 */

package com.pqcs.impergram.music;

import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MusicArtworkCache {

    public static final String MISSING = "\u0000missing";

    private static final int MEMORY_CACHE_SIZE = 128;
    private static final long MAX_DISK_CACHE_BYTES = 32L * 1024 * 1024;
    private static final long MAX_FILE_BYTES = 4L * 1024 * 1024;
    private static final String DIR_NAME = "music_art";

    private static volatile MusicArtworkCache instance;

    public static MusicArtworkCache getInstance() {
        if (instance == null) {
            synchronized (MusicArtworkCache.class) {
                if (instance == null) instance = new MusicArtworkCache();
            }
        }
        return instance;
    }

    private final LruCache<String, String> memory = new LruCache<>(MEMORY_CACHE_SIZE);
    private final File dir;
    private final Object diskLock = new Object();

    private MusicArtworkCache() {
        File cacheRoot = ApplicationLoader.applicationContext.getCacheDir();
        this.dir = new File(cacheRoot, DIR_NAME);
        if (!dir.exists())
            dir.mkdirs();
    }

    @Nullable
    public String get(@NonNull String key) {
        String mem = memory.get(key);
        if (mem != null) return mem;

        File f = fileForKey(key);
        if (f.exists() && f.length() > 0) {
            String path = f.getAbsolutePath();
            memory.put(key, path);
            return path;
        }
        return null;
    }

    public boolean isMissing(@NonNull String key) {
        return MISSING.equals(memory.get(key));
    }

    public void markMissing(@NonNull String key) {
        memory.put(key, MISSING);
    }

    @Nullable
    public String put(@NonNull String key, @NonNull InputStream in) {
        File f = fileForKey(key);
        try {
            try (FileOutputStream fos = new FileOutputStream(f)) {
                byte[] buf = new byte[8192];
                int n;
                long total = 0;
                while ((n = in.read(buf)) > 0) {
                    fos.write(buf, 0, n);
                    total += n;
                    if (total > MAX_FILE_BYTES) break;
                }
            }
            if (f.length() == 0) {
                f.delete();
                return null;
            }
            String path = f.getAbsolutePath();
            memory.put(key, path);
            trimIfNeeded();
            return path;
        } catch (IOException e) {
            FileLog.e(e);
            f.delete();
            return null;
        }
    }

    public static String keyFor(@Nullable String title, @Nullable String artist) {
        String t = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
        String a = artist == null ? "" : artist.trim().toLowerCase(Locale.ROOT);
        return sha1(a + "\u0001" + t);
    }

    private File fileForKey(String key) {
        return new File(dir, key + ".jpg");
    }

    private void trimIfNeeded() {
        synchronized (diskLock) {
            File[] files = dir.listFiles();
            if (files == null || files.length < 2) return;
            long total = 0;
            for (File f : files) total += f.length();
            if (total <= MAX_DISK_CACHE_BYTES) return;

            List<File> list = new ArrayList<>(Arrays.asList(files));
            list.sort(Comparator.comparingLong(File::lastModified));
            long target = MAX_DISK_CACHE_BYTES * 3 / 4;
            for (File f : list) {
                if (total <= target) break;
                long len = f.length();
                if (f.delete()) total -= len;
            }
        }
    }

    private static String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}