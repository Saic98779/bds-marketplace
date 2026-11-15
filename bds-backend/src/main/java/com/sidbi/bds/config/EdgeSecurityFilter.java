package com.sidbi.bds.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-IP rate limiting and common response headers, applied before Spring Security.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EdgeSecurityFilter extends OncePerRequestFilter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int authRequestsPerMinute;
    private final int apiRequestsPerMinute;

    public EdgeSecurityFilter(@Value("${bds.web.auth-requests-per-minute:30}") int authRequestsPerMinute,
                              @Value("${bds.web.api-requests-per-minute:600}") int apiRequestsPerMinute) {
        this.authRequestsPerMinute = authRequestsPerMinute;
        this.apiRequestsPerMinute = apiRequestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String clientIp = clientIp(request);
        boolean authPath = path.startsWith("/api/v1/auth/");
        String bucket = (authPath ? "auth:" : "api:") + clientIp;
        int limit = authPath ? authRequestsPerMinute : apiRequestsPerMinute;
        if (!allow(bucket, limit)) {
            reject(response, HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please retry shortly.", path);
            return;
        }
        response.setHeader("X-Request-Id", UUID.randomUUID().toString());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        chain.doFilter(request, response);
    }

    private boolean allow(String key, int limit) {
        long minute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(key, (k, existing) ->
                existing == null || existing.minute != minute ? new Window(minute) : existing);
        if (windows.size() > 50_000) {
            windows.entrySet().removeIf(e -> e.getValue().minute != minute);
        }
        return window.counter.incrementAndGet() <= limit;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static void reject(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"success\":false,\"status\":" + status.value() + ",\"message\":\"" + message
                + "\",\"timestamp\":\"" + Instant.now() + "\",\"path\":\"" + path.replace("\"", "") + "\"}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    private static final class Window {
        private final long minute;
        private final AtomicInteger counter = new AtomicInteger();

        private Window(long minute) {
            this.minute = minute;
        }
    }
}
