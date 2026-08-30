package io.quorumforge.flashpoint;

import java.net.URI;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

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
        }
        System.out.println("PASS: health endpoint integration test");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
