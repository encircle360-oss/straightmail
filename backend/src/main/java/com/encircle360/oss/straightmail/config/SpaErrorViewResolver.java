package com.encircle360.oss.straightmail.config;

import com.encircle360.oss.straightmail.util.ApiPaths;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * Serves the Angular admin console for browser routes that have no server-side handler, so that
 * client-side routing survives a reload or a deep link.
 *
 * <p>This runs on the 404 path rather than as a catch-all {@code @RequestMapping}, which matters
 * for two reasons: it knows the configured {@code api.prefix}, so an unknown endpoint under the API
 * still answers a real 404 instead of a page of HTML, and it has no fixed limit on how deep a route
 * may be nested.
 */
@Component
public class SpaErrorViewResolver implements ErrorViewResolver {

    @Value("${api.prefix:/api}")
    private String apiPrefix;

    /**
     * Forwards to the SPA entry point for unresolved browser navigation, and leaves every other
     * error to the default handling.
     *
     * @param request the error dispatch, carrying the original URI as a request attribute
     * @param status  the status the original request failed with
     * @param model   the error attributes (unused)
     * @return a forward to {@code /index.html} with status 200, or {@code null} to fall through
     */
    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        if (status != HttpStatus.NOT_FOUND || !this.isSpaRoute(request) || !this.wantsHtml(request)) {
            return null;
        }

        ModelAndView forward = new ModelAndView("forward:/index.html");
        // The route exists — the client router owns it — so the browser must not see a 404.
        forward.setStatus(HttpStatus.OK);
        return forward;
    }

    /**
     * Returns whether the original URI looks like an Angular route: not under the API prefix and
     * not a static file. Missing assets keep their 404 so a broken reference stays visible.
     */
    private boolean isSpaRoute(HttpServletRequest request) {
        Object original = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String path = original instanceof String uri ? uri : request.getRequestURI();

        if (ApiPaths.isApiPath(path, apiPrefix) || path.startsWith("/actuator")) {
            return false;
        }

        int lastSegment = path.lastIndexOf('/');
        return path.indexOf('.', lastSegment + 1) < 0;
    }

    /**
     * Returns whether the caller asked for HTML, so that an API client receives the JSON error body
     * instead of a page it cannot use.
     */
    private boolean wantsHtml(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept == null || accept.isBlank()) {
            return false;
        }
        return MediaType.parseMediaTypes(accept).stream()
                .anyMatch(MediaType.TEXT_HTML::isCompatibleWith);
    }
}
