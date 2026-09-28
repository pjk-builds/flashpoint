package io.quorumforge.flashpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe in-process inventory used by the order admission path. */
final class Inventory {
    private final ConcurrentHashMap<String, Stock> stock = new ConcurrentHashMap<>();

    void restock(String sku, int quantity) {
        validate(sku, quantity);
        stock.compute(sku, (ignored, current) -> {
            if (current == null) return new Stock(quantity);
            current.add(quantity);
            return current;
        });
    }

    boolean reserve(String sku, int quantity) {
        validate(sku, quantity);
        Stock item = stock.get(sku);
        return item != null && item.reserve(quantity);
    }

    int available(String sku) {
        if (sku == null || sku.isBlank()) return 0;
        Stock item = stock.get(sku);
        return item == null ? 0 : item.available();
    }

    Map<String, Integer> snapshot() {
        var result = new java.util.TreeMap<String, Integer>();
        stock.forEach((sku, item) -> result.put(sku, item.available()));
        return Map.copyOf(result);
    }

    private static void validate(String sku, int quantity) {
        if (sku == null || sku.isBlank() || quantity < 1) {
            throw new IllegalArgumentException("sku and positive quantity are required");
        }
    }

    private static final class Stock {
        private int available;

        private Stock(int available) {
            this.available = available;
        }

        private synchronized void add(int quantity) {
            available = Math.addExact(available, quantity);
        }

        private synchronized boolean reserve(int quantity) {
            if (available < quantity) return false;
            available -= quantity;
            return true;
        }

        private synchronized int available() {
            return available;
        }
    }
}
