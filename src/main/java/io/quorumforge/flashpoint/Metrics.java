package io.quorumforge.flashpoint;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Prometheus text exposition for the in-process service. */
final class Metrics {
    private final ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();
    private final LongAdder latencyNanos = new LongAdder();
    private final LongAdder timedRequests = new LongAdder();

    void increment(String name) {
        counters.computeIfAbsent(name, ignored -> new LongAdder()).increment();
    }

    void observe(long nanos) {
        latencyNanos.add(nanos);
        timedRequests.increment();
    }

    String prometheus(int inFlight, int inventory, int orders) {
        var out = new StringBuilder();
        counters.forEach((name, value) -> out.append("flashpoint_requests_total{result=\"")
            .append(name).append("\"} ").append(value.sum()).append('\n'));
        out.append("flashpoint_request_duration_seconds_sum ")
            .append(latencyNanos.sum() / 1_000_000_000.0).append('\n');
        out.append("flashpoint_request_duration_seconds_count ")
            .append(timedRequests.sum()).append('\n');
        out.append("flashpoint_in_flight ").append(inFlight).append('\n');
        out.append("flashpoint_inventory_available ").append(inventory).append('\n');
        out.append("flashpoint_orders_total ").append(orders).append('\n');
        return out.toString();
    }
}
