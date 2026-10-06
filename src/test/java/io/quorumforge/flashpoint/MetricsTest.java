package io.quorumforge.flashpoint;

public final class MetricsTest {
    public static void main(String[] args) {
        var metrics = new Metrics();
        metrics.increment("accepted");
        metrics.increment("accepted");
        metrics.increment("rate_limited");
        metrics.observe(1_000_000_000L);

        String output = metrics.prometheus(2, 7, 3);
        check(output.contains("flashpoint_requests_total{result=\"accepted\"} 2"),
            "accepted counter missing");
        check(output.contains("flashpoint_requests_total{result=\"rate_limited\"} 1"),
            "rate limit counter missing");
        check(output.contains("flashpoint_request_duration_seconds_count 1"),
            "request count missing");
        check(output.contains("flashpoint_in_flight 2"), "in-flight gauge missing");
        check(output.contains("flashpoint_inventory_available 7"), "inventory gauge missing");
        check(output.contains("flashpoint_orders_total 3"), "order gauge missing");
        System.out.println("PASS: metrics tests");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
