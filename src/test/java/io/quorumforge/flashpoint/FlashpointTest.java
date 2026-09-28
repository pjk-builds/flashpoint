package io.quorumforge.flashpoint;

import java.net.URI;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;

public final class FlashpointTest {
    public static void main(String[] args) throws Exception {
        try (var server = new FlashpointServer(0)) {
            server.start();

            var connection = (HttpURLConnection) URI
                .create("http://localhost:" + server.port() + "/health")
                .toURL()
                .openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1_000);
            connection.setReadTimeout(1_000);

            int status = connection.getResponseCode();
            String contentType = connection.getHeaderField("Content-Type");
            String body;
            try (var responseBody = connection.getInputStream()) {
                body = new String(responseBody.readAllBytes(), StandardCharsets.UTF_8);
            } finally {
                connection.disconnect();
            }

            check(status == 200, "expected status 200");
            check(body.equals("{\"status\":\"UP\"}"), "unexpected response body");
            check("application/json; charset=utf-8".equals(contentType), "unexpected content type");

            String baseUrl = "http://localhost:" + server.port();
            HttpURLConnection restock = connection(baseUrl + "/inventory", "PUT");
            restock.setDoOutput(true);
            restock.setRequestProperty("Content-Type", "application/json");
            try (OutputStream requestBody = restock.getOutputStream()) {
                requestBody.write("{\"sku\":\"console\",\"quantity\":3}".getBytes(StandardCharsets.UTF_8));
            }
            check(restock.getResponseCode() == 200, "expected restock status 200");
            restock.disconnect();

            HttpURLConnection inventory = connection(baseUrl + "/inventory", "GET");
            check(inventory.getResponseCode() == 200, "expected inventory status 200");
            String inventoryBody;
            try (var responseBody = inventory.getInputStream()) {
                inventoryBody = new String(responseBody.readAllBytes(), StandardCharsets.UTF_8);
            } finally {
                inventory.disconnect();
            }
            check(inventoryBody.equals("{\"console\":3}"), "unexpected inventory response: " + inventoryBody);
        }
        System.out.println("PASS: health endpoint integration test");
    }

    private static HttpURLConnection connection(String url, String method) throws Exception {
        var connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(1_000);
        connection.setReadTimeout(1_000);
        return connection;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
