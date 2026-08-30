package io.quorumforge.flashpoint;

public final class Flashpoint {
    private Flashpoint() {}

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        FlashpointServer server = new FlashpointServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.start();
        System.out.printf("Flashpoint listening on http://localhost:%d%n", server.port());
    }
}
