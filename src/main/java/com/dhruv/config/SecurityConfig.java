package com.dhruv.config;

import com.dhruv.web.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Central security policy. Before this existed the application had no authentication
 * layer at all — every {@code /api/v1/**} endpoint was reachable anonymously and the
 * caller's identity came from a request parameter.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RateLimitFilter rateLimitFilter;

    /**
     * Comma-separated exact origins, e.g. {@code https://app.dhruv.in,https://www.dhruv.in}.
     * Wildcards are rejected at startup in the prod profile — see {@link StartupConfigValidator}.
     */
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * BCrypt at the Spring Security default strength (10). Passwords were previously
     * stored and compared as plaintext.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Tracks live sessions per principal so the one-session-per-account limit can be
     * enforced. In-memory, and therefore per instance — see the README note on running
     * more than one replica.
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Required for the registry to hear about session expiry and avoid leaking entries. */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        // Hand the raw token to the SPA rather than deferring it: the frontend reads the
        // token from the login/session response body, because a cross-origin deployment
        // cannot read the XSRF-TOKEN cookie from JavaScript.
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfHandler)
                // The WebSocket handshake is a GET and is origin-checked separately by
                // WebSocketConfig; Spring does not apply CSRF to GET in any case.
                .ignoringRequestMatchers("/ws/**"))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.changeSessionId())
                // One concurrent session per account; the oldest is expired on re-login.
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry))
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000)))
            .authorizeHttpRequests(auth -> auth
                // --- Public: authentication entry points and infrastructure probes ---
                .requestMatchers("/api/v1/auth/student", "/api/v1/auth/parent").permitAll()
                .requestMatchers("/api/v1/auth/logout", "/api/v1/auth/session", "/api/v1/auth/csrf").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/ws/**").permitAll()

                // --- Parent-only: reports about linked children ---
                .requestMatchers("/api/v1/parent/**").hasRole("PARENT")

                // --- Student-only: a student's own readiness data and plan. A parent
                //     session reaching these would resolve its own id as a student id. ---
                .requestMatchers("/api/v1/student/**").hasRole("STUDENT")
                .requestMatchers("/api/v1/readiness/**").hasRole("STUDENT")
                .requestMatchers("/api/v1/plan/**").hasRole("STUDENT")

                // --- Everything else requires a session ---
                .anyRequest().authenticated())
            // Return 401 JSON instead of redirecting an XHR to a login page.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> writeError(res, HttpStatus.UNAUTHORIZED,
                        "UNAUTHENTICATED", "Sign in to continue."))
                .accessDeniedHandler((req, res, deniedEx) -> writeError(res, HttpStatus.FORBIDDEN,
                        "FORBIDDEN", "You do not have access to this resource.")))
            .anonymous(Customizer.withDefaults())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable()) // handled by AuthController so the SPA gets JSON
            .addFilterBefore(rateLimitFilter, BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // Credentials are required for the session cookie, so origins must be an exact
        // allowlist. "*" with allowCredentials(true) is rejected by the CORS spec and was
        // silently disabling credentialed requests before.
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN", "X-Requested-With"));
        config.setExposedHeaders(List.of("X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static void writeError(jakarta.servlet.http.HttpServletResponse res,
                                   HttpStatus status, String code, String message) throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
