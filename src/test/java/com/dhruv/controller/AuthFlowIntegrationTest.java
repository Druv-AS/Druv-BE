package com.dhruv.controller;

import com.dhruv.repository.ParentReportRepository;
import com.dhruv.repository.ParentRepository;
import com.dhruv.repository.StudentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end sign-up, session, and sign-out over HTTP, exercising the real filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ParentRepository parentRepository;
    @Autowired private ParentReportRepository parentReportRepository;
    @Autowired private ServerProperties serverProperties;

    private static final String PHONE = "9876600001";
    private static final String PASSWORD = "IntegrationPass9";

    /** CSRF token obtained the way the real client does, before any POST. */
    private String csrfToken;
    private Cookie csrfCookie;

    @BeforeEach
    void clean() throws Exception {
        parentReportRepository.deleteAll();
        studentRepository.deleteAll();
        parentRepository.deleteAll();

        MvcResult bootstrap = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        csrfToken = objectMapper.readTree(bootstrap.getResponse().getContentAsString())
                .path("csrfToken").asText();
        csrfCookie = bootstrap.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfToken).as("bootstrap must issue a CSRF token").isNotBlank();
    }

    /** Applies the bootstrap CSRF token to a request, as the browser client would. */
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withCsrf(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        builder.header("X-XSRF-TOKEN", csrfToken);
        if (csrfCookie != null) {
            builder.cookie(csrfCookie);
        }
        return builder;
    }

    @Test
    @DisplayName("register, then use the session cookie to read protected data, then sign out")
    void fullSessionLifecycle() throws Exception {
        MvcResult registration = mockMvc.perform(withCsrf(post("/api/v1/auth/student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andReturn();

        // MockMvc does not run the container's cookie writer, so the session is asserted
        // through the session object the request created. Cookie flags are covered by
        // sessionCookieIsHardened() below.
        MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);
        assertThat(session).as("authentication must create a server-side session").isNotNull();

        // The credential is the session, not the response body.
        mockMvc.perform(get("/api/v1/readiness/eri").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallEri").isNumber());

        mockMvc.perform(get("/api/v1/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        mockMvc.perform(withCsrf(post("/api/v1/auth/logout")).session(session))
                .andExpect(status().isOk());

        // After logout the same session must no longer authorise anything.
        assertThat(session.isInvalid()).as("logout must invalidate the session").isTrue();
        mockMvc.perform(get("/api/v1/readiness/eri").session(new MockHttpSession()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the session cookie is HttpOnly and its SameSite policy is set explicitly")
    void sessionCookieIsHardened() {
        var cookie = serverProperties.getServlet().getSession().getCookie();

        assertThat(cookie.getName()).isEqualTo("DHRUVSESSION");
        assertThat(cookie.getHttpOnly()).as("must be unreadable from JavaScript").isTrue();
        assertThat(cookie.getSameSite()).as("SameSite must be explicit, not browser default").isNotNull();
    }

    @Test
    @DisplayName("the password is never echoed back in any auth response")
    void responseNeverContainsPassword() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/auth/student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(PHONE)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(PASSWORD);
        assertThat(body).doesNotContain("\"password\"");
        assertThat(body).doesNotContain("$2a$");
    }

    @Test
    @DisplayName("the session endpoint returns 401, not a 500 or an empty body, when signed out")
    void sessionEndpointRejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("a short password is rejected with per-field validation detail")
    void rejectsShortPassword() throws Exception {
        String body = """
                {"mode":"register","name":"Test","phoneNumber":"9876600002","password":"short"}
                """;

        mockMvc.perform(withCsrf(post("/api/v1/auth/student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.password").isNotEmpty());
    }

    @Test
    @DisplayName("wrong credentials return 401 with a non-committal message")
    void wrongCredentialsReturn401() throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/auth/student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(PHONE)))
                .andExpect(status().isOk());

        String body = """
                {"mode":"login","phoneNumber":"%s","password":"WrongPassword1"}
                """.formatted(PHONE);

        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/auth/student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.path("message").asText())
                .as("must not reveal whether the account exists")
                .doesNotContain("not found")
                .doesNotContain("does not exist");
    }

    @Test
    @DisplayName("signing in issues a new session id, defeating session fixation")
    void sessionIdRotatesOnLogin() throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/auth/student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(PHONE)))
                .andExpect(status().isOk());

        // A session the attacker planted in the victim's browser before sign-in.
        MockHttpSession preAuthSession = new MockHttpSession();
        String fixatedId = preAuthSession.getId();

        MvcResult login = mockMvc.perform(withCsrf(post("/api/v1/auth/student"))
                        .session(preAuthSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode":"login","phoneNumber":"%s","password":"%s"}
                                """.formatted(PHONE, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        var authenticatedSession = login.getRequest().getSession(false);
        assertThat(authenticatedSession).isNotNull();
        assertThat(authenticatedSession.getId())
                .as("the pre-authentication session id must not survive sign-in")
                .isNotEqualTo(fixatedId);
    }

    private static String registrationBody(String phone) {
        return """
                {"mode":"register","name":"Integration Student","phoneNumber":"%s",
                 "parentPhoneNumber":"9876600099","examTarget":"NEET 2027 Repeater","password":"%s"}
                """.formatted(phone, PASSWORD);
    }

}
