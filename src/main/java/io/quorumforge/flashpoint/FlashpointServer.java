package io.quorumforge.flashpoint;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class FlashpointServer implements AutoCloseable {
    private static final byte[] HEALTH_RESPONSE = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);

    private final HttpServer server;

    FlashpointServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", this::handleHealth);
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

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length);
        try (var responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }
}
