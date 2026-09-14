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
import androidx.annotation.UiThread;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class MusicArtworkLoader {

    @UiThread
    public interface Listener {
        void onArtworkLoaded(@Nullable String title, @Nullable String artist, @Nullable String path);
    }

    private static final int THREADS = 4;

    private static volatile MusicArtworkLoader instance;

    public static MusicArtworkLoader getInstance() {
        if (instance == null) {
            synchronized (MusicArtworkLoader.class) {
                if (instance == null) instance = new MusicArtworkLoader();
            }
        }
        return instance;
    }

    private final MusicArtworkCache cache = MusicArtworkCache.getInstance();
    private final ExecutorService pool;
    private final ConcurrentHashMap<String, List<Listener>> inFlight = new ConcurrentHashMap<>();

    private MusicArtworkLoader() {
        final AtomicInteger counter = new AtomicInteger(1);
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "music-art-" + counter.getAndIncrement());
            t.setPriority(Thread.MIN_PRIORITY);
            t.setDaemon(true);
            return t;
        };
        this.pool = new ThreadPoolExecutor(
                THREADS, THREADS,
                30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                tf
        );
    }

    public void load(@Nullable String title, @Nullable String artist, @NonNull Listener listener) {
        if (title == null || title.isEmpty()) {
            AndroidUtilities.runOnUIThread(() -> listener.onArtworkLoaded(title, artist, null));
            return;
        }

        final String key = MusicArtworkCache.keyFor(title, artist);

        String cachedPath = cache.get(key);
        if (cachedPath != null && !MusicArtworkCache.MISSING.equals(cachedPath)) {
            final String path = cachedPath;
            AndroidUtilities.runOnUIThread(() -> listener.onArtworkLoaded(title, artist, path));
            return;
        }
        if (cache.isMissing(key)) {
            AndroidUtilities.runOnUIThread(() -> listener.onArtworkLoaded(title, artist, null));
            return;
        }

        List<Listener> listeners = new ArrayList<>(2);
        listeners.add(listener);
        List<Listener> existing = inFlight.putIfAbsent(key, listeners);
        if (existing != null) {
            synchronized (existing) {
                existing.add(listener);
            }
            return;
        }

        final String fTitle = title;
        final String fArtist = artist;
        try {
            pool.execute(() -> {
                String path = null;
                try {
                    path = MusicArtworkProvider.findAndDownload(fTitle, fArtist, cache, key);
                } catch (Throwable t) {
                    FileLog.e(t);
                }
                if (path == null) cache.markMissing(key);
                final String resultPath = path;
                AndroidUtilities.runOnUIThread(() ->
                        dispatchResult(key, fTitle, fArtist, resultPath));
            });
        } catch (Throwable t) {
            FileLog.e(t);
            inFlight.remove(key);
            AndroidUtilities.runOnUIThread(() -> listener.onArtworkLoaded(title, artist, null));
        }
    }

    public void cancel(@Nullable String title, @Nullable String artist, @NonNull Listener listener) {
        if (title == null || title.isEmpty()) return;
        String key = MusicArtworkCache.keyFor(title, artist);
        List<Listener> listeners = inFlight.get(key);
        if (listeners != null) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
    }

    private void dispatchResult(String key, String title, String artist, String path) {
        List<Listener> listeners = inFlight.remove(key);
        if (listeners == null) return;
        List<Listener> snapshot;
        synchronized (listeners) {
            snapshot = new ArrayList<>(listeners);
        }
        for (Listener l : snapshot) {
            try {
                l.onArtworkLoaded(title, artist, path);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }
}