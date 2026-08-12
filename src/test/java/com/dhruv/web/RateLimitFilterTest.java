package com.dhruv.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private static final int MAX_ATTEMPTS = 3;

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "maxAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(filter, "windowSeconds", 300L);
        ReflectionTestUtils.setField(filter, "trustForwardedFor", false);
        chain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("allows attempts up to the limit, then blocks with 429")
    void blocksOnceTheLimitIsReached() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(loginRequest("203.0.113.5"), response, chain);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }
        verify(chain, times(MAX_ATTEMPTS)).doFilter(any(), any());

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest("203.0.113.5"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(blocked.getContentAsString()).contains("RATE_LIMITED");
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
        verify(chain, times(MAX_ATTEMPTS)).doFilter(any(), any()); // not called again
    }

    @Test
    @DisplayName("limits are tracked per client, so one attacker cannot lock out everyone")
    void limitsAreScopedPerClient() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS + 2; i++) {
            filter.doFilter(loginRequest("203.0.113.5"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse other = new MockHttpServletResponse();
        filter.doFilter(loginRequest("198.51.100.7"), other, chain);

        assertThat(other.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("only the credential endpoints are throttled")
    void doesNotThrottleOtherEndpoints() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS + 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/readiness/eri");
            request.setServletPath("/api/v1/readiness/eri");
            request.setRemoteAddr("203.0.113.5");

            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }
    }

    @Test
    @DisplayName("X-Forwarded-For is ignored unless the proxy is explicitly trusted")
    void ignoresForwardedHeaderByDefault() throws Exception {
        // A client rotating the header must not get a fresh quota each time.
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            MockHttpServletRequest request = loginRequest("203.0.113.5");
            request.addHeader("X-Forwarded-For", "10.0.0." + i);
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest spoofed = loginRequest("203.0.113.5");
        spoofed.addHeader("X-Forwarded-For", "10.0.0.99");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(spoofed, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    @DisplayName("when the proxy is trusted, the forwarded client address is the limit key")
    void usesForwardedHeaderWhenTrusted() throws Exception {
        ReflectionTestUtils.setField(filter, "trustForwardedFor", true);

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            MockHttpServletRequest request = loginRequest("10.0.0.1"); // the proxy
            request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest sameClient = loginRequest("10.0.0.1");
        sameClient.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(sameClient, blocked, chain);
        assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        MockHttpServletRequest differentClient = loginRequest("10.0.0.1");
        differentClient.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        MockHttpServletResponse allowed = new MockHttpServletResponse();
        filter.doFilter(differentClient, allowed, chain);
        assertThat(allowed.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("GET on the auth path is not throttled; only credential submission is")
    void onlyThrottlesPostRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/student");
        request.setServletPath("/api/v1/auth/student");
        request.setRemoteAddr("203.0.113.5");

        for (int i = 0; i < MAX_ATTEMPTS + 3; i++) {
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }
        verify(chain, times(MAX_ATTEMPTS + 3)).doFilter(any(), any());
        verify(chain, never()).doFilter(null, null);
    }

    private static MockHttpServletRequest loginRequest(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/student");
        request.setServletPath("/api/v1/auth/student");
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
