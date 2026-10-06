package io.quorumforge.flashpoint;

public final class TokenBucketTest {
    public static void main(String[] args) throws Exception {
        rejectsRequestsBeyondBurst();
        refillsTokensOverTime();
        System.out.println("PASS: token bucket tests");
    }

    private static void rejectsRequestsBeyondBurst() {
        var bucket = new TokenBucket(1, 2);
        check(bucket.tryAcquire(), "first burst request should pass");
        check(bucket.tryAcquire(), "second burst request should pass");
        check(!bucket.tryAcquire(), "request beyond burst should be rejected");
    }

    private static void refillsTokensOverTime() throws InterruptedException {
        var bucket = new TokenBucket(100, 1);
        check(bucket.tryAcquire(), "initial request should pass");
        check(!bucket.tryAcquire(), "bucket should be empty");
        Thread.sleep(25);
        check(bucket.tryAcquire(), "bucket should refill over time");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
