package io.quorumforge.flashpoint;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Coordinates idempotent order decisions with inventory reservations. */
final class OrderService {
    enum Status { ACCEPTED, SOLD_OUT }

    record Order(String orderId, String idempotencyKey, String sku, int quantity,
                 Status status, Instant createdAt) {}

    private final Inventory inventory;
    private final ConcurrentHashMap<String, Order> ordersByKey = new ConcurrentHashMap<>();

    OrderService(Inventory inventory) {
        this.inventory = inventory;
    }

    Order place(String idempotencyKey, String sku, int quantity) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        if (sku == null || sku.isBlank() || quantity < 1) {
            throw new IllegalArgumentException("sku and positive quantity are required");
        }

        return ordersByKey.compute(idempotencyKey, (key, existing) -> {
            if (existing != null) {
                if (!existing.sku().equals(sku) || existing.quantity() != quantity) {
                    throw new IdempotencyConflict("idempotency key was already used with a different request");
                }
                return existing;
            }

            Status status = inventory.reserve(sku, quantity) ? Status.ACCEPTED : Status.SOLD_OUT;
            return new Order(UUID.randomUUID().toString(), key, sku, quantity, status, Instant.now());
        });
    }

    static final class IdempotencyConflict extends RuntimeException {
        IdempotencyConflict(String message) {
            super(message);
        }
    }
}
