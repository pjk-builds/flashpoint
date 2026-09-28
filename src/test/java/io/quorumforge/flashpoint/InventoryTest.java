package io.quorumforge.flashpoint;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class InventoryTest {
    public static void main(String[] args) throws Exception {
        reservesOnlyAvailableStock();
        reservesAtomicallyUnderContention();
        rejectsInvalidStockChanges();
        System.out.println("PASS: inventory tests");
    }

    private static void reservesOnlyAvailableStock() {
        var inventory = new Inventory();
        inventory.restock("console", 2);

        check(inventory.reserve("console", 1), "first reservation should succeed");
        check(inventory.available("console") == 1, "one item should remain");
        check(!inventory.reserve("console", 2), "reservation should fail when stock is insufficient");
        check(inventory.available("console") == 1, "failed reservation must not change stock");
    }

    private static void reservesAtomicallyUnderContention() throws Exception {
        var inventory = new Inventory();
        inventory.restock("console", 100);
        var pool = Executors.newFixedThreadPool(32);
        var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();

        for (int i = 0; i < 1_000; i++) {
            futures.add(pool.submit(() -> inventory.reserve("console", 1)));
        }

        pool.shutdown();
        check(pool.awaitTermination(10, TimeUnit.SECONDS), "workers did not finish");
        long accepted = 0;
        for (var future : futures) {
            if (future.get()) accepted++;
        }

        check(accepted == 100, "expected exactly 100 reservations, got " + accepted);
        check(inventory.available("console") == 0, "stock must reach zero without becoming negative");
    }

    private static void rejectsInvalidStockChanges() {
        var inventory = new Inventory();
        expectIllegalArgument(() -> inventory.restock("", 1));
        expectIllegalArgument(() -> inventory.restock("console", 0));
        expectIllegalArgument(() -> inventory.reserve("console", -1));
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected invalid input to be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
