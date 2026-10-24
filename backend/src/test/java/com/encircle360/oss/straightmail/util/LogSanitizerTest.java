package com.encircle360.oss.straightmail.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins that a request-controlled value cannot forge or recolour a log line.
 */
class LogSanitizerTest {

    @Test
    void replacesLineBreaks() {
        assertEquals("welcome__INFO forged", LogSanitizer.sanitize("welcome\r\nINFO forged"));
    }

    @Test
    void replacesOtherControlCharacters() {
        assertEquals("_[31mred", LogSanitizer.sanitize("\u001B[31mred"));
    }

    @Test
    void leavesOrdinaryValuesUntouched() {
        assertEquals("acme-corp/welcome.ftl", LogSanitizer.sanitize("acme-corp/welcome.ftl"));
    }

    @Test
    void passesNullThrough() {
        assertNull(LogSanitizer.sanitize(null));
    }
}
