package com.rexcantor64.triton.performance;

import com.rexcantor64.triton.Triton;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance load scaling manager designed for high player counts (100-500+ active players).
 * <p>
 * Dynamically adjusts packet processing aggressiveness and fast-path string scanning to prevent
 * MSPT degradation, ping spikes, and thread bloat under heavy server load.
 *
 * @since 4.1.0
 */
public class AdaptiveLoadManager {

    private static final AdaptiveLoadManager INSTANCE = new AdaptiveLoadManager();

    public enum LoadTier {
        /** Normal operations: < 100 players */
        NORMAL,
        /** Moderate load: 100 - 300 players */
        MODERATE,
        /** Heavy load: 300 - 500+ players */
        HEAVY
    }

    @Getter
    private volatile LoadTier currentTier = LoadTier.NORMAL;
    private final AtomicInteger cachedPlayerCount = new AtomicInteger(0);
    private final Map<String, Boolean> stringSkipCache = new ConcurrentHashMap<>(1024, 0.75f, 16);
    private static final int MAX_CACHE_SIZE = 4096;

    public static AdaptiveLoadManager get() {
        return INSTANCE;
    }

    /**
     * Recalculates current load tier based on active player count.
     */
    public void updatePlayerCount(int onlinePlayers) {
        cachedPlayerCount.set(onlinePlayers);
        if (onlinePlayers >= 300) {
            currentTier = LoadTier.HEAVY;
        } else if (onlinePlayers >= 100) {
            currentTier = LoadTier.MODERATE;
        } else {
            currentTier = LoadTier.NORMAL;
        }
    }

    /**
     * Fast-path check to determine if a string can skip full translation parsing.
     * Evaluates whether the string contains any Triton translation syntax or placeholder indicators.
     *
     * @param text Raw string to inspect
     * @return true if string contains NO translation tags and can be safely skipped
     */
    public boolean shouldQuickSkip(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        // Short strings without special characters are guaranteed non-translatable
        if (text.length() < 3 && text.indexOf('[') == -1 && text.indexOf('%') == -1) {
            return true;
        }

        // Tier 2 & Tier 3: Check cache for high-frequency identical strings
        if (currentTier != LoadTier.NORMAL && text.length() <= 128) {
            Boolean cached = stringSkipCache.get(text);
            if (cached != null) {
                return cached;
            }
        }

        boolean canSkip = text.indexOf('[') == -1 && text.indexOf('%') == -1 && !text.contains("triton");
        if (currentTier != LoadTier.NORMAL && text.length() <= 128) {
            if (stringSkipCache.size() > MAX_CACHE_SIZE) {
                stringSkipCache.clear();
            }
            stringSkipCache.put(text, canSkip);
        }
        return canSkip;
    }

    /**
     * Clear memoization caches on reload or world transition.
     */
    public void clearCache() {
        stringSkipCache.clear();
    }
}
