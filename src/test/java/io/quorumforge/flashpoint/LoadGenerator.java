package io.quorumforge.flashpoint;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/** Small repeatable client for measuring order endpoint behavior under load. */
public final class LoadGenerator {
    public static void main(String[] args) throws Exception {
        int requests = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;
        int concurrency = args.length > 1 ? Integer.parseInt(args[1]) : 100;
        String baseUrl = args.length > 2 ? args[2] : "http://localhost:8080";
        String runId = Long.toUnsignedString(System.nanoTime());
        var httpPool = Executors.newFixedThreadPool(Math.min(concurrency, 32));
        var client = HttpClient.newBuilder().executor(httpPool)
            .connectTimeout(Duration.ofSeconds(2)).build();
        var pool = Executors.newFixedThreadPool(concurrency);
        var next = new AtomicInteger();
        var statuses = new ConcurrentHashMap<Integer, LongAdder>();
        var latencies = Collections.synchronizedList(new ArrayList<Long>(requests));
        long suiteStart = System.nanoTime();

        for (int worker = 0; worker < concurrency; worker++) {
            pool.submit(() -> {
                int index;
                while ((index = next.getAndIncrement()) < requests) {
                    var request = HttpRequest.newBuilder(URI.create(baseUrl + "/orders"))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .header("Idempotency-Key", "load-" + runId + "-" + index)
                        .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"sku\":\"console\",\"quantity\":1}"))
                        .build();
                    long started = System.nanoTime();
                    try {
                        int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                        statuses.computeIfAbsent(status, ignored -> new LongAdder()).increment();
                    } catch (Exception error) {
                        statuses.computeIfAbsent(0, ignored -> new LongAdder()).increment();
                    }
                    latencies.add(System.nanoTime() - started);
                }
            });
        }

        pool.shutdown();
        if (!pool.awaitTermination(2, TimeUnit.MINUTES)) {
            pool.shutdownNow();
            throw new IllegalStateException("load workers did not finish");
        }
        httpPool.shutdownNow();

        double seconds = (System.nanoTime() - suiteStart) / 1_000_000_000.0;
        latencies.sort(Long::compare);
        System.out.printf("requests=%d concurrency=%d duration=%.2fs throughput=%.0f RPS%n",
            requests, concurrency, seconds, requests / seconds);
        System.out.printf("p50=%.2fms p95=%.2fms p99=%.2fms statuses=%s%n",
            percentile(latencies, .50), percentile(latencies, .95), percentile(latencies, .99), printable(statuses));
    }

    private static double percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0;
        int index = Math.min(values.size() - 1, (int) Math.ceil(values.size() * percentile) - 1);
        return values.get(index) / 1_000_000.0;
    }

    private static Map<Integer, Long> printable(Map<Integer, LongAdder> values) {
        var result = new java.util.TreeMap<Integer, Long>();
        values.forEach((key, value) -> result.put(key, value.sum()));
        return result;
    }
}
