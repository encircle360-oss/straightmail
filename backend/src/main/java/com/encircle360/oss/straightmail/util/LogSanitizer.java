package com.encircle360.oss.straightmail.util;

/**
 * Neutralises request-controlled values before they are written to the log.
 *
 * <p>Template IDs, tenant slugs, locales and paths arrive from the caller. A line break in one of
 * them would let the caller forge a log entry of their own; other control characters (ANSI escapes
 * among them) can rewrite what an operator sees in a terminal.
 *
 * <p>Not instantiable.
 */
public class LogSanitizer {

    // Private constructor to hide implicit public one.
    private LogSanitizer() {
    }

    /**
     * Replaces line breaks and all other control characters with {@code _}.
     *
     * @param value the value to log; {@code null} is returned as-is
     * @return the value, safe to place in a single log line
     */
    public static String sanitize(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString()
                .replaceAll("[\\r\\n]", "_")
                .replaceAll("\\p{Cntrl}", "_");
    }
}
