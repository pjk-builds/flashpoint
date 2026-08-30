package io.quorumforge.flashpoint;

public final class Flashpoint {
    private Flashpoint() {}

    static String startupMessage() {
        return "Flashpoint is ready for its first feature.";
    }

    public static void main(String[] args) {
        System.out.println(startupMessage());
    }
}
