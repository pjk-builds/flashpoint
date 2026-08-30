package io.quorumforge.flashpoint;

public final class FlashpointTest {
    public static void main(String[] args) {
        String message = Flashpoint.startupMessage();
        if (!"Flashpoint is ready for its first feature.".equals(message)) {
            throw new AssertionError("unexpected startup message: " + message);
        }
        System.out.println("PASS: bootstrap smoke test");
    }
}
