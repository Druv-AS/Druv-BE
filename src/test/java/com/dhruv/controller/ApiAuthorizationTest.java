package com.dhruv.controller;

import com.dhruv.domain.StudentEntity;
import com.dhruv.repository.ParentReportRepository;
import com.dhruv.repository.ParentRepository;
import com.dhruv.repository.StudentRepository;
import com.dhruv.security.AppRole;
import com.dhruv.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint-level authorization. Each test here corresponds to a hole that was open before
 * Spring Security was introduced: every one of these requests previously returned 200.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ParentRepository parentRepository;
    @Autowired private ParentReportRepository parentReportRepository;

    private StudentEntity student;

    @BeforeEach
    void setUp() {
        parentReportRepository.deleteAll();
        studentRepository.deleteAll();
        parentRepository.deleteAll();

        student = studentRepository.save(new StudentEntity(
                "test_student", "+919876500101", "+919876500102", "Test Student", "NEET 2027 Repeater"));
    }

    // ------------------------------------------------------------------ anonymous access

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/readiness/eri",
            "/api/v1/readiness/heatmap",
            "/api/v1/readiness/backlog-debt",
            "/api/v1/readiness/parent-report",
            "/api/v1/parent/students",
            "/api/v1/plan/daily",
            "/api/v1/costudy/room-state",
    })
    @DisplayName("every data endpoint rejects anonymous callers")
    void anonymousCannotReadData(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("anonymous callers cannot link a student to an arbitrary parent")
    void anonymousCannotLinkStudent() throws Exception {
        mockMvc.perform(post("/api/v1/parent/link-student")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIdentifier\":\"test_student\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("anonymous callers cannot release another student's report")
    void anonymousCannotSendReport() throws Exception {
        mockMvc.perform(post("/api/v1/student/send-report").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------------------------- cross-role access

    @Test
    @DisplayName("a student session cannot read the parent portal")
    void studentCannotAccessParentPortal() throws Exception {
        mockMvc.perform(get("/api/v1/parent/students").with(asStudent(student)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("a parent session cannot read student readiness data")
    void parentCannotAccessStudentReadiness() throws Exception {
        mockMvc.perform(get("/api/v1/readiness/eri").with(asParent()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a parent session cannot release a student's report")
    void parentCannotSendStudentReport() throws Exception {
        mockMvc.perform(post("/api/v1/student/send-report").with(csrf()).with(asParent()))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------------------- the IDOR defects

    @Test
    @DisplayName("regression: a parent only sees children who nominated their number")
    void parentSeesOnlyTheirOwnChildren() throws Exception {
        // This student nominated a different parent.
        studentRepository.save(new StudentEntity(
                "other_student", "+919876500201", "+919876500999", "Other Student", "JEE Advanced 2027"));

        mockMvc.perform(get("/api/v1/parent/students")
                        .with(asPrincipal(new AppUserPrincipal(
                                java.util.UUID.randomUUID(), "parent_x", "+919876500102", AppRole.PARENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentUserId").value("test_student"));
    }

    @Test
    @DisplayName("regression: a parent cannot link a student who did not nominate them")
    void parentCannotClaimAnUnrelatedStudent() throws Exception {
        studentRepository.save(new StudentEntity(
                "unrelated", "+919876500301", "+919876500888", "Unrelated", "NEET 2027 Repeater"));

        mockMvc.perform(post("/api/v1/parent/link-student")
                        .with(csrf())
                        .with(asPrincipal(new AppUserPrincipal(
                                java.util.UUID.randomUUID(), "attacker", "+919876500777", AppRole.PARENT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIdentifier\":\"unrelated\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LINK_NOT_AUTHORISED"));
    }

    @Test
    @DisplayName("a parent can link a student who did nominate them")
    void parentCanLinkANominatingStudent() throws Exception {
        mockMvc.perform(post("/api/v1/parent/link-student")
                        .with(csrf())
                        .with(asPrincipal(new AppUserPrincipal(
                                java.util.UUID.randomUUID(), "parent_ok", "+919876500102", AppRole.PARENT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIdentifier\":\"test_student\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("send-report acts on the caller's own record, not one named in the request")
    void sendReportIsScopedToTheSession() throws Exception {
        mockMvc.perform(post("/api/v1/student/send-report")
                        .with(csrf())
                        .with(asStudent(student))
                        // A leftover parameter from the old API must not redirect the action.
                        .param("studentPhoneOrId", "other_student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentUserId").value("test_student"));
    }

    // ------------------------------------------------------- parent contact nomination

    @Test
    @DisplayName("a student can nominate their parent, which then permits the link")
    void studentNominationEnablesLinking() throws Exception {
        StudentEntity unlinked = studentRepository.save(new StudentEntity(
                "nominator", "+919876500401", null, "Nominator", "NEET 2027 Repeater"));

        // Before nomination the parent cannot link.
        mockMvc.perform(post("/api/v1/parent/link-student")
                        .with(csrf())
                        .with(asPrincipal(new AppUserPrincipal(
                                java.util.UUID.randomUUID(), "p1", "+919876500402", AppRole.PARENT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIdentifier\":\"nominator\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/student/parent-contact")
                        .with(csrf())
                        .with(asStudent(unlinked))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentPhoneNumber\":\"9876500402\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentPhoneNumber").value("+919876500402"));

        // After nomination the same request succeeds.
        mockMvc.perform(post("/api/v1/parent/link-student")
                        .with(csrf())
                        .with(asPrincipal(new AppUserPrincipal(
                                java.util.UUID.randomUUID(), "p1", "+919876500402", AppRole.PARENT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIdentifier\":\"nominator\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a student can only change their own parent contact")
    void parentContactIsScopedToTheSession() throws Exception {
        StudentEntity victim = studentRepository.save(new StudentEntity(
                "victim", "+919876500501", "+919876500502", "Victim", "NEET 2027 Repeater"));

        // The signed-in student is `student`, not `victim`; nothing in the request can
        // redirect the write to another account.
        mockMvc.perform(put("/api/v1/student/parent-contact")
                        .with(csrf())
                        .with(asStudent(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentPhoneNumber\":\"9876500777\"}"))
                .andExpect(status().isOk());

        assertThat(studentRepository.findById(victim.getId()))
                .get()
                .extracting(StudentEntity::getParentPhoneNumber)
                .isEqualTo("+919876500502");
    }

    @Test
    void rejectsAMalformedParentContact() throws Exception {
        mockMvc.perform(put("/api/v1/student/parent-contact")
                        .with(csrf())
                        .with(asStudent(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentPhoneNumber\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARENT_PHONE"));
    }

    // ------------------------------------------------------------------------- CSRF

    @Test
    @DisplayName("state-changing requests without a CSRF token are rejected")
    void mutationsRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/student/send-report").with(asStudent(student)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------- helpers

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asStudent(StudentEntity s) {
        return asPrincipal(new AppUserPrincipal(s.getId(), s.getUserId(), s.getPhoneNumber(), AppRole.STUDENT));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asParent() {
        return asPrincipal(new AppUserPrincipal(
                java.util.UUID.randomUUID(), "some_parent", "+919876500102", AppRole.PARENT));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asPrincipal(AppUserPrincipal p) {
        return authentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(p, null, List.of(new SimpleGrantedAuthority(p.getRole().authority()))));
    }
}
