package io.quorumforge.flashpoint;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

final class FlashpointServer implements AutoCloseable {
    private static final byte[] HEALTH_RESPONSE = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);

    private final Inventory inventory = new Inventory();
    private final OrderService orders = new OrderService(inventory);
    private final TokenBucket rateLimiter;
    private final Semaphore capacity;
    private final ExecutorService executor;
    private final HttpServer server;

    FlashpointServer(int port) throws IOException {
        this(port, 5_000, 1_000, 64);
    }

    FlashpointServer(int port, int requestsPerSecond, int burst, int maxInFlight) throws IOException {
        if (maxInFlight < 1) throw new IllegalArgumentException("max in-flight must be positive");
        rateLimiter = new TokenBucket(requestsPerSecond, burst);
        capacity = new Semaphore(maxInFlight);
        executor = Executors.newFixedThreadPool(maxInFlight);
        server = HttpServer.create(new InetSocketAddress(port), 256);
        server.setExecutor(executor);
        server.createContext("/health", this::handleHealth);
        server.createContext("/inventory", this::handleInventory);
        server.createContext("/orders", this::handleOrders);
    }

    void start() {
        server.start();
    }

    int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/health")) {
            respond(exchange, 404, new byte[0]);
            return;
        }
        if (!exchange.getRequestMethod().equals("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            respond(exchange, 405, new byte[0]);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        respond(exchange, 200, HEALTH_RESPONSE);
    }

    private void handleInventory(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            StringBuilder json = new StringBuilder("{");
            inventory.snapshot().forEach((sku, value) ->
                json.append(Json.quote(sku)).append(':').append(value).append(','));
            if (json.length() > 1) json.setLength(json.length() - 1);
            respond(exchange, 200, json.append('}').toString(), "application/json; charset=utf-8");
            return;
        }

        if ("PUT".equals(exchange.getRequestMethod())) {
            try {
                Map<String, String> body = body(exchange);
                String sku = body.get("sku");
                int quantity = positiveInt(body.get("quantity"));
                inventory.restock(sku, quantity);
                respond(exchange, 200,
                    "{\"sku\":" + Json.quote(sku) + ",\"available\":" + inventory.available(sku) + "}",
                    "application/json; charset=utf-8");
            } catch (IllegalArgumentException error) {
                respond(exchange, 400, "{\"error\":" + Json.quote(error.getMessage()) + "}",
                    "application/json; charset=utf-8");
            }
            return;
        }

        exchange.getResponseHeaders().set("Allow", "GET, PUT");
        respond(exchange, 405, "{\"error\":\"method not allowed\"}", "application/json; charset=utf-8");
    }

    private void handleOrders(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "POST");
            respond(exchange, 405, "{\"error\":\"method not allowed\"}", "application/json; charset=utf-8");
            return;
        }

        if (!rateLimiter.tryAcquire()) {
            respond(exchange, 429, "{\"error\":\"request rate exceeded\"}", "application/json; charset=utf-8");
            return;
        }
        if (!capacity.tryAcquire()) {
            respond(exchange, 503, "{\"error\":\"server at capacity\"}", "application/json; charset=utf-8");
            return;
        }

        try {
            Map<String, String> request = body(exchange);
            OrderService.Order order = orders.place(
                exchange.getRequestHeaders().getFirst("Idempotency-Key"),
                request.get("sku"),
                positiveInt(request.get("quantity")));
            String response = "{\"orderId\":" + Json.quote(order.orderId())
                + ",\"sku\":" + Json.quote(order.sku())
                + ",\"quantity\":" + order.quantity()
                + ",\"status\":" + Json.quote(order.status().name()) + "}";
            int status = order.status() == OrderService.Status.ACCEPTED ? 201 : 409;
            respond(exchange, status, response, "application/json; charset=utf-8");
        } catch (OrderService.IdempotencyConflict error) {
            respond(exchange, 409, "{\"error\":" + Json.quote(error.getMessage()) + "}",
                "application/json; charset=utf-8");
        } catch (IllegalArgumentException error) {
            respond(exchange, 400, "{\"error\":" + Json.quote(error.getMessage()) + "}",
                "application/json; charset=utf-8");
        } finally {
            capacity.release();
        }
    }

    private static Map<String, String> body(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(16_385);
        if (bytes.length > 16_384) throw new IllegalArgumentException("body exceeds 16 KiB");
        return Json.parseFlatObject(new String(bytes, StandardCharsets.UTF_8));
    }

    private static int positiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) throw new NumberFormatException();
            return parsed;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("quantity must be a positive integer");
        }
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length);
        try (var responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        respond(exchange, status, body.getBytes(StandardCharsets.UTF_8), contentType);
    }

    private static void respond(HttpExchange exchange, int status, byte[] body, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        respond(exchange, status, body);
    }
}
