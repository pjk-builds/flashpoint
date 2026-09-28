package io.quorumforge.flashpoint;

public final class OrderServiceTest {
    public static void main(String[] args) {
        acceptsOrdersAndReportsSoldOut();
        replaysIdempotentOrders();
        rejectsChangedIdempotentRequests();
        System.out.println("PASS: order service tests");
    }

    private static void acceptsOrdersAndReportsSoldOut() {
        var inventory = new Inventory();
        inventory.restock("console", 1);
        var orders = new OrderService(inventory);

        check(orders.place("buyer-1", "console", 1).status() == OrderService.Status.ACCEPTED,
            "available inventory should be accepted");
        check(orders.place("buyer-2", "console", 1).status() == OrderService.Status.SOLD_OUT,
            "empty inventory should be sold out");
    }

    private static void replaysIdempotentOrders() {
        var inventory = new Inventory();
        inventory.restock("gpu", 2);
        var orders = new OrderService(inventory);
        var first = orders.place("same-key", "gpu", 1);
        var replay = orders.place("same-key", "gpu", 1);

        check(first.orderId().equals(replay.orderId()), "replay must return the original order");
        check(inventory.available("gpu") == 1, "replay must not reserve inventory twice");
    }

    private static void rejectsChangedIdempotentRequests() {
        var inventory = new Inventory();
        inventory.restock("gpu", 3);
        var orders = new OrderService(inventory);
        orders.place("same-key", "gpu", 1);

        try {
            orders.place("same-key", "gpu", 2);
            throw new AssertionError("changed request should conflict");
        } catch (OrderService.IdempotencyConflict expected) {
            check(inventory.available("gpu") == 2, "conflict must not mutate inventory");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
