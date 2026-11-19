package com.bookmyjuice.config;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rejects malformed webhook bodies with <b>400</b> instead of letting them explode as a 500.
 *
 * <p>Every webhook controller starts with {@code Event.retrieve(e.get("id").toString())}.
 * When the body has no {@code id}, that throws a {@code NullPointerException}, the controller's
 * catch-all turns it into a 500, and Chargebee keeps retrying a request that can never succeed.
 * A 500 also hides genuine client errors from the delivery log.
 *
 * <p>Implemented as a filter so all thirteen controllers are covered without editing them, and
 * ordered last so Spring Security authenticates first (an unauthenticated caller still gets 401,
 * which is the correct answer before the payload even matters).
 *
 * <p>The body is buffered and replayed, so controllers still read it normally.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class WebhookPayloadValidationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(WebhookPayloadValidationFilter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/webhooks/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        byte[] body = request.getInputStream().readAllBytes();
        String problem = validate(body);

        if (problem != null) {
            logger.warn("Rejected malformed webhook request to {}: {}", request.getRequestURI(), problem);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"status\":\"error\",\"error\":\"malformed_webhook_payload\","
                    + "\"message\":\"" + problem + "\"}");
            return;
        }

        chain.doFilter(new CachedBodyRequest(request, body), response);
    }

    /**
     * Validate a webhook body.
     *
     * @return {@code null} when the body is usable, otherwise a human-readable reason
     */
    static String validate(byte[] body) {
        if (body == null || body.length == 0) {
            return "empty request body";
        }
        JsonNode node;
        try {
            node = MAPPER.readTree(body);
        } catch (Exception e) {
            return "body is not valid JSON";
        }
        if (node == null || !node.isObject()) {
            return "body must be a JSON object";
        }
        JsonNode id = node.get("id");
        if (id == null || id.isNull() || id.asText().isBlank()) {
            return "missing event id";
        }
        return null;
    }

    /** Replays the buffered body so downstream handlers can still read it. */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream source = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return source.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return source.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return source.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(jakarta.servlet.ReadListener readListener) {
                    // Synchronous reads only.
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
