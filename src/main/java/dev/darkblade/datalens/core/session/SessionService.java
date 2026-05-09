package dev.darkblade.datalens.core.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.darkblade.datalens.model.InspectableObject;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages per-player {@link PlayerSession} instances backed by a Caffeine LRU cache.
 * Sessions expire after the configured TTL of inactivity.
 */
public final class SessionService {

    private final Cache<UUID, PlayerSession> cache;

    public SessionService(long ttlSeconds, long maxSessions) {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSessions)
                .build();
    }

    /**
     * Opens (or replaces) a session for the given player.
     */
    public PlayerSession open(UUID playerId, InspectableObject object) {
        PlayerSession session = new PlayerSession(object);
        cache.put(playerId, session);
        return session;
    }

    /**
     * Returns the player's current session, if any.
     */
    public Optional<PlayerSession> get(UUID playerId) {
        return Optional.ofNullable(cache.getIfPresent(playerId));
    }

    /**
     * Closes and invalidates the player's session.
     */
    public void close(UUID playerId) {
        cache.invalidate(playerId);
    }

    /**
     * Returns the number of active sessions.
     */
    public long activeCount() {
        return cache.estimatedSize();
    }
}
