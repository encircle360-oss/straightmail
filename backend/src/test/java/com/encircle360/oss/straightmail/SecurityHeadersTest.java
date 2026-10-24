package com.encircle360.oss.straightmail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the response security headers.
 *
 * <p>The admin SPA is served from the same origin as the API and keeps the access token in browser
 * storage, so a script injected into that origin could read it. The Content-Security-Policy is the
 * control that makes that injection hard; these tests fail if it is dropped or silently narrowed to
 * the point where the shipped UI would break.
 */
@SpringBootTest(classes = StraightmailApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles({"database", "test"})
class SecurityHeadersTest extends AbstractTest {

    @Test
    void responses_carry_a_content_security_policy() throws Exception {
        MvcResult result = get("/v1/info", status().isOk());
        String csp = result.getResponse().getHeader("Content-Security-Policy");

        assertNotNull(csp, "every response must carry a Content-Security-Policy");
        assertTrue(csp.contains("default-src 'self'"), csp);
        assertTrue(csp.contains("script-src 'self'"), "script-src must stay strict: " + csp);
        assertTrue(csp.contains("object-src 'none'"), csp);
        assertTrue(csp.contains("frame-ancestors 'none'"), csp);
    }

    @Test
    void content_security_policy_allows_what_the_shipped_ui_actually_loads() throws Exception {
        MvcResult result = get("/v1/info", status().isOk());
        String csp = result.getResponse().getHeader("Content-Security-Policy");

        // index.html pulls the stylesheet and font files from Google Fonts
        assertTrue(csp.contains("https://fonts.googleapis.com"), csp);
        assertTrue(csp.contains("https://fonts.gstatic.com"), csp);
        // user-avatar.component.ts builds Gravatar URLs
        assertTrue(csp.contains("https://www.gravatar.com"), csp);
        // Angular injects component styles as inline style elements
        assertTrue(csp.contains("style-src 'self' 'unsafe-inline'"), csp);
    }

    @Test
    void responses_carry_the_remaining_hardening_headers() throws Exception {
        MvcResult result = get("/v1/info", status().isOk());

        assertEquals("nosniff", result.getResponse().getHeader("X-Content-Type-Options"));
        assertEquals("DENY", result.getResponse().getHeader("X-Frame-Options"));
        assertEquals("same-origin", result.getResponse().getHeader("Referrer-Policy"));
    }
}
