package com.dhruv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

        log.info("Production configuration validated: durable datasource, strict CORS, secure cookies.");
    }

    private static boolean containsLocalhost(String origins) {
        String lower = origins.toLowerCase();
        return lower.contains("localhost") || lower.contains("127.0.0.1") || lower.contains("[::1]");
    }
}
