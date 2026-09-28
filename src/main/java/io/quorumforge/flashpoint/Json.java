package io.quorumforge.flashpoint;

import java.util.HashMap;
import java.util.Map;

/** Small dependency-free JSON helper for the deliberately flat API payloads. */
final class Json {
    private Json() {}

    static Map<String, String> parseFlatObject(String input) {
        var result = new HashMap<String, String>();
        String text = input == null ? "" : input.trim();
        if (!text.startsWith("{") || !text.endsWith("}")) {
            throw new IllegalArgumentException("body must be a JSON object");
        }
        text = text.substring(1, text.length() - 1).trim();
        if (text.isEmpty()) return result;
        for (String pair : text.split(",")) {
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) throw new IllegalArgumentException("invalid JSON field");
            result.put(unquote(parts[0].trim()), unquote(parts[1].trim()));
        }
        return result;
    }

    static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
