package io.quorumforge.flashpoint;

public final class Flashpoint {
    private Flashpoint() {}

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        int rate = env("RATE_LIMIT_RPS", 5_000);
        int burst = env("RATE_LIMIT_BURST", 1_000);
        int maxInFlight = env("MAX_IN_FLIGHT", 64);
        FlashpointServer server = new FlashpointServer(port, rate, burst, maxInFlight);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.start();
        System.out.printf("Flashpoint listening on http://localhost:%d%n", server.port());
    }

    private static int env(String name, int fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : Integer.parseInt(value);
    }
}
