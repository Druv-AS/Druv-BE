package com.dhruv.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The production guard rails. These assert that a misconfigured deploy fails at startup
 * rather than silently coming up on an in-memory database or a wildcard CORS policy.
 */
class StartupConfigValidatorTest {

    @Test
    @DisplayName("a fully safe production configuration starts")
    void acceptsSafeConfiguration() {
        assertThatCode(() -> validator(safe()).validate())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("refuses the in-memory H2 default, which loses all data on restart")
    void rejectsInMemoryDatabase() {
        Config config = safe();
        config.datasourceUrl = "jdbc:h2:mem:dhruvdb;MODE=PostgreSQL";

        assertThatThrownBy(() -> validator(config).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_DATASOURCE_URL");
    }

    @Test
    void rejectsBlankDatasource() {
        Config config = safe();
        config.datasourceUrl = "";

        assertThatThrownBy(() -> validator(config).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_DATASOURCE_URL");
    }

    @Test
    @DisplayName("refuses ddl-auto=update, which lets Hibernate alter production tables")
    void rejectsSchemaMutatingDdlAuto() {
        Config config = safe();
        config.ddlAuto = "update";

        assertThatThrownBy(() -> validator(config).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ddl-auto");
    }

    @Test
    @DisplayName("refuses wildcard CORS, which cannot be combined with credentials")
    void rejectsWildcardCors() {
        Config config = safe();
        config.corsOrigins = "*";

        assertThatThrownBy(() -> validator(config).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORS_ALLOWED_ORIGINS");
    }

    @Test
    @DisplayName("refuses a leftover localhost origin, which would block the real frontend")
    void rejectsLocalhostCors() {
        Config config = safe();
        config.corsOrigins = "https://app.dhruv.example.com,http://localhost:5173";

        assertThatThrownBy(() -> validator(config).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localhost");
    }

    @Test
    void rejectsWildcardWebsocketOrigins() {
        Config config = safe();
        config.websocketOrigins = "*";

        assertThatThrownBy(() -> validator(config).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("websocket");
    }

    @Test
    @DisplayName("refuses a session cookie that may travel over plaintext HTTP")
    void rejectsInsecureCookie() {
        Config config = safe();
        config.cookieSecure = false;

        assertThatThrownBy(() -> validator(config).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("secure");
    }

    @Test
    @DisplayName("every problem is reported at once, not one per restart")
    void reportsAllProblemsTogether() {
        Config config = safe();
        config.datasourceUrl = "";
        config.corsOrigins = "*";
        config.cookieSecure = false;

        assertThatThrownBy(() -> validator(config).validate())
                .isInstanceOf(IllegalStateException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .contains("SPRING_DATASOURCE_URL")
                        .contains("CORS_ALLOWED_ORIGINS")
                        .contains("secure"));
    }

    // ------------------------------------------------- SameSite / cross-site warning

    @Test
    @DisplayName("a cross-site frontend with SameSite=Lax warns but still starts")
    void warnsWhenSameSiteWouldDropTheCookie() {
        // The real Vercel + Railway deployment: unrelated registrable domains.
        Config config = safe();
        config.cookieSameSite = "Lax";
        config.publicOrigin = "https://druv-be-production-88a4.up.railway.app";
        config.corsOrigins = "https://druv-fe-git-main-druva.vercel.app";

        // A warning, not a failure: the check is a heuristic, so it must not block a deploy.
        assertThatCode(() -> validator(config).validate()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("subdomains of one domain are same-site, so Lax is correct there")
    void acceptsLaxForSameSiteSubdomains() {
        Config config = safe();
        config.cookieSameSite = "Lax";
        config.publicOrigin = "https://api.dhruv.example.com";
        config.corsOrigins = "https://app.dhruv.example.com";

        assertThatCode(() -> validator(config).validate()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the SameSite check is skipped when the public origin is unknown")
    void skipsSameSiteCheckWithoutPublicOrigin() {
        Config config = safe();
        config.cookieSameSite = "Lax";
        config.publicOrigin = "";

        assertThatCode(() -> validator(config).validate()).doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------------- helpers

    /** Mutable holder so each test can spoil exactly one field of a valid configuration. */
    private static final class Config {
        String datasourceUrl;
        String ddlAuto;
        String corsOrigins;
        String websocketOrigins;
        boolean cookieSecure;
        String cookieSameSite;
        String publicOrigin;
    }

    private static Config safe() {
        Config config = new Config();
        config.datasourceUrl = "jdbc:postgresql://db.internal:5432/dhruv?sslmode=require";
        config.ddlAuto = "validate";
        config.corsOrigins = "https://app.dhruv.example.com";
        config.websocketOrigins = "https://app.dhruv.example.com";
        config.cookieSecure = true;
        config.cookieSameSite = "None";
        config.publicOrigin = "https://api.dhruv.example.com";
        return config;
    }

    private static StartupConfigValidator validator(Config config) {
        StartupConfigValidator validator = new StartupConfigValidator();
        ReflectionTestUtils.setField(validator, "datasourceUrl", config.datasourceUrl);
        ReflectionTestUtils.setField(validator, "ddlAuto", config.ddlAuto);
        ReflectionTestUtils.setField(validator, "corsOrigins", config.corsOrigins);
        ReflectionTestUtils.setField(validator, "websocketOrigins", config.websocketOrigins);
        ReflectionTestUtils.setField(validator, "cookieSecure", config.cookieSecure);
        ReflectionTestUtils.setField(validator, "cookieSameSite", config.cookieSameSite);
        ReflectionTestUtils.setField(validator, "publicOrigin", config.publicOrigin);
        return validator;
    }
}
