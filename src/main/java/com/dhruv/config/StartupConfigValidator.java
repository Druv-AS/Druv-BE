package com.dhruv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Refuses to serve traffic when the production profile is misconfigured.
 *
 * <p>The defaults in {@code application.yml} are development defaults: an in-memory H2
 * database and a localhost CORS origin. Deploying with those defaults silently in place
 * meant every restart discarded all user data. Failing loudly at startup is strictly
 * better than discovering that in production.
 */
@Component
@Profile("prod")
public class StartupConfigValidator implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupConfigValidator.class);

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.jpa.hibernate.ddl-auto:}")
    private String ddlAuto;

    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String corsOrigins;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.websocket.allowed-origins:}")
    private String websocketOrigins;

    @Value("${server.servlet.session.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * The API's own public origin, used only to tell a same-site deployment apart from a
     * cross-site one when checking the SameSite policy. Optional; the check is skipped
     * when it is not set.
     */
    @Value("${app.public-origin:}")
    private String publicOrigin;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        validate();
    }

    /**
     * Checks the configuration, throwing if anything is unsafe.
     *
     * <p>Separate from the event callback so it can be tested without constructing a
     * Spring context or an {@link ApplicationReadyEvent}.
     *
     * @throws IllegalStateException listing every problem found, not just the first
     */
    void validate() {
        List<String> problems = new ArrayList<>();

        if (datasourceUrl.isBlank() || datasourceUrl.startsWith("jdbc:h2:")) {
            problems.add("SPRING_DATASOURCE_URL must point at a durable PostgreSQL database; "
                    + "the in-memory H2 default loses all data on restart.");
        }
        if (!"validate".equalsIgnoreCase(ddlAuto) && !"none".equalsIgnoreCase(ddlAuto)) {
            problems.add("spring.jpa.hibernate.ddl-auto must be 'validate' or 'none' in production "
                    + "(found '" + ddlAuto + "'); Flyway owns the schema.");
        }
        if (corsOrigins.isBlank()) {
            problems.add("CORS_ALLOWED_ORIGINS must list the exact frontend origins.");
        } else if (corsOrigins.contains("*")) {
            problems.add("CORS_ALLOWED_ORIGINS must not contain a wildcard; the session cookie "
                    + "requires credentialed CORS, which forbids '*'.");
        } else if (containsLocalhost(corsOrigins)) {
            // Not a hole, but it means the real frontend is blocked. Better to fail at
            // startup than to ship a build where every API call is a CORS error.
            problems.add("CORS_ALLOWED_ORIGINS still contains a localhost development origin.");
        }

        if (websocketOrigins.isBlank() || websocketOrigins.contains("*")) {
            problems.add("app.websocket.allowed-origins must list exact origins, not a wildcard.");
        } else if (containsLocalhost(websocketOrigins)) {
            problems.add("app.websocket.allowed-origins still contains a localhost development origin.");
        }
        if (!cookieSecure) {
            problems.add("server.servlet.session.cookie.secure must be true so the session cookie "
                    + "is never sent over plaintext HTTP.");
        }

        if (!problems.isEmpty()) {
            String detail = String.join("\n  - ", problems);
            log.error("Refusing to start with an unsafe production configuration:\n  - {}", detail);
            throw new IllegalStateException("Unsafe production configuration:\n  - " + detail);
        }

        warnIfSameSiteWillDropTheCookie();

        log.info("Production configuration validated: durable datasource, strict CORS, secure cookies.");
    }

    /**
     * Warns when the frontend is on a different site than the API but the session cookie
     * is not {@code SameSite=None}.
     *
     * <p>This combination fails in the most confusing way available: sign-in returns 200
     * and sets the cookie, the browser then declines to send it on any cross-site request,
     * and every subsequent call 401s. It reads as "sessions don't persist" rather than as
     * a cookie policy problem.
     *
     * <p>A warning rather than a failure, because the registrable domain cannot be derived
     * reliably without a public suffix list, and {@code app.public-origin} is optional.
     */
    private void warnIfSameSiteWillDropTheCookie() {
        if ("none".equalsIgnoreCase(cookieSameSite) || publicOrigin.isBlank() || corsOrigins.isBlank()) {
            return;
        }

        String apiHost = hostOf(publicOrigin);
        boolean anyCrossSite = Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(StartupConfigValidator::hostOf)
                .anyMatch(frontendHost -> !sharesRegistrableDomain(frontendHost, apiHost));

        if (anyCrossSite) {
            log.warn("""
                    SESSION_COOKIE_SAMESITE is '{}' but a frontend origin in CORS_ALLOWED_ORIGINS \
                    is on a different site than this API ({}). Browsers will not send the session \
                    cookie on those requests, so sign-in will appear to succeed and every later \
                    call will return 401. Set SESSION_COOKIE_SAMESITE=None (with SESSION_COOKIE_SECURE=true), \
                    or host the frontend and API as subdomains of one domain.""",
                    cookieSameSite, apiHost);
        }
    }

    private static String hostOf(String origin) {
        try {
            String host = java.net.URI.create(origin.trim()).getHost();
            return host == null ? origin.trim() : host;
        } catch (IllegalArgumentException e) {
            return origin.trim();
        }
    }

    /**
     * Approximates same-site by comparing the last two labels. Good enough to catch the
     * vercel.app-versus-railway.app case; it deliberately does not consult a public suffix
     * list, which is why this only ever warns.
     */
    private static boolean sharesRegistrableDomain(String a, String b) {
        return lastTwoLabels(a).equalsIgnoreCase(lastTwoLabels(b));
    }

    private static String lastTwoLabels(String host) {
        String[] labels = host.split("\\.");
        return labels.length < 2 ? host : labels[labels.length - 2] + "." + labels[labels.length - 1];
    }

    private static boolean containsLocalhost(String origins) {
        String lower = origins.toLowerCase();
        return lower.contains("localhost") || lower.contains("127.0.0.1") || lower.contains("[::1]");
    }
}
