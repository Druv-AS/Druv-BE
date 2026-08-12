package com.dhruv.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Throttles the unauthenticated authentication endpoints, which were previously open to
 * unlimited credential stuffing.
 *
 * <p>Deliberately dependency-free and in-memory: the limit is per instance. That is a real
 * limitation behind a load balancer — with N instances the effective ceiling is N times
 * the configured limit. It raises the cost of an attack rather than eliminating it; a
 * shared Redis-backed limiter is the correct next step and is noted in the README.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Cap on distinct tracked keys, so a spoofed-IP flood cannot exhaust heap. */
    private static final int MAX_TRACKED_KEYS = 50_000;

    @Value("${security.rate-limit.auth.max-attempts:10}")
    private int maxAttempts;

    @Value("${security.rate-limit.auth.window-seconds:300}")
    private long windowSeconds;

    /**
     * Set to true only when the app genuinely runs behind a trusted proxy that overwrites
     * X-Forwarded-For. Honouring the header otherwise lets a client forge its own identity
     * and bypass the limit entirely.
     */
    @Value("${security.rate-limit.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> lastSweep = new AtomicReference<>(Instant.EPOCH);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only the credential-accepting endpoints are throttled. Authenticated traffic is
        // already bounded by the session, and throttling reads would break normal use.
        String path = request.getServletPath();
        return !("POST".equalsIgnoreCase(request.getMethod())
                && (path.equals("/api/v1/auth/student") || path.equals("/api/v1/auth/parent")));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String key = clientKey(request);
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofSeconds(windowSeconds));

        sweepIfDue(now, cutoff);

        Deque<Instant> window = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        boolean limited;
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                window.pollFirst();
            }
            limited = window.size() >= maxAttempts;
            if (!limited) {
                window.addLast(now);
            }
        }

        if (limited) {
            log.warn("Rate limit exceeded for {} on {}", key, request.getServletPath());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":"
                    + "\"Too many attempts. Please wait a few minutes and try again.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /** Drops fully expired windows at most once per window, keeping the map bounded. */
    private void sweepIfDue(Instant now, Instant cutoff) {
        Instant last = lastSweep.get();
        if (now.isBefore(last.plus(Duration.ofSeconds(windowSeconds)))
                && attempts.size() < MAX_TRACKED_KEYS) {
            return;
        }
        if (!lastSweep.compareAndSet(last, now)) {
            return; // another thread is sweeping
        }
        attempts.entrySet().removeIf(entry -> {
            Deque<Instant> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });
    }

    private String clientKey(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // Left-most entry is the originating client.
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
