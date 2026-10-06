package io.quorumforge.flashpoint;

/** Thread-safe token bucket for protecting the order endpoint from bursts. */
final class TokenBucket {
    private final double capacity;
    private final double refillPerNano;
    private double tokens;
    private long lastRefill;

    TokenBucket(int requestsPerSecond, int burst) {
        if (requestsPerSecond < 1 || burst < 1) {
            throw new IllegalArgumentException("rate and burst must be positive");
        }
        capacity = burst;
        tokens = burst;
        refillPerNano = requestsPerSecond / 1_000_000_000.0;
        lastRefill = System.nanoTime();
    }

    synchronized boolean tryAcquire() {
        long now = System.nanoTime();
        tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerNano);
        lastRefill = now;
        if (tokens < 1) return false;
        tokens--;
        return true;
    }
}
