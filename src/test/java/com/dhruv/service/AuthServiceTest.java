package com.dhruv.service;

import com.dhruv.domain.ParentEntity;
import com.dhruv.domain.StudentEntity;
import com.dhruv.dto.ParentAuthDto;
import com.dhruv.dto.StudentAuthDto;
import com.dhruv.repository.ParentReportRepository;
import com.dhruv.repository.ParentRepository;
import com.dhruv.repository.StudentRepository;
import com.dhruv.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioural tests for registration and sign-in, with emphasis on the takeover and
 * plaintext-password defects that existed before hashing was introduced.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired private AuthService authService;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ParentRepository parentRepository;
    @Autowired private ParentReportRepository parentReportRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String PHONE = "9876500001";
    private static final String PARENT_PHONE = "9876500002";
    private static final String PASSWORD = "CorrectHorse7";

    @BeforeEach
    void clean() {
        parentReportRepository.deleteAll();
        studentRepository.deleteAll();
        parentRepository.deleteAll();
    }

    // ------------------------------------------------------------ password handling

    @Test
    @DisplayName("registration stores a BCrypt hash, never the plaintext password")
    void registrationHashesPassword() {
        StudentEntity student = authService.authenticateStudent(register(PHONE, PASSWORD));

        assertThat(student.getPassword())
                .isNotEqualTo(PASSWORD)
                .startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, student.getPassword())).isTrue();
    }

    @Test
    @DisplayName("two accounts with the same password get different hashes (salted)")
    void hashesAreSalted() {
        StudentEntity first = authService.authenticateStudent(register(PHONE, PASSWORD));
        StudentEntity second = authService.authenticateStudent(register("9876500003", PASSWORD));

        assertThat(first.getPassword()).isNotEqualTo(second.getPassword());
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        authService.authenticateStudent(register(PHONE, PASSWORD));

        StudentEntity loggedIn = authService.authenticateStudent(login(PHONE, PASSWORD));

        assertThat(loggedIn.getPhoneNumber()).isEqualTo("+91" + PHONE);
    }

    @Test
    void loginFailsWithWrongPassword() {
        authService.authenticateStudent(register(PHONE, PASSWORD));

        assertThatThrownBy(() -> authService.authenticateStudent(login(PHONE, "WrongPassword1")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("INVALID_CREDENTIALS"));
    }

    // --------------------------------------------------------------- the takeover bug

    @Nested
    @DisplayName("regression: password-less accounts")
    class PasswordlessAccounts {

        @Test
        @DisplayName("login against an account with no password fails instead of claiming it")
        void loginDoesNotClaimPasswordlessStudent() {
            // Simulates a row created before passwords were mandatory. The old code set the
            // supplied password onto the account and returned success, handing the account
            // to whoever tried first.
            StudentEntity legacy = new StudentEntity(
                    "legacy_user", "+919876500009", null, "Legacy", "NEET 2027 Repeater");
            legacy.setPassword(null);
            studentRepository.save(legacy);

            assertThatThrownBy(() -> authService.authenticateStudent(login("9876500009", "AttackerPass1")))
                    .isInstanceOf(ApiException.class)
                    .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("INVALID_CREDENTIALS"));

            assertThat(studentRepository.findByUserId("legacy_user"))
                    .get()
                    .extracting(StudentEntity::getPassword)
                    .isNull();
        }

        @Test
        @DisplayName("the same protection applies to parent accounts")
        void loginDoesNotClaimPasswordlessParent() {
            ParentEntity legacy = new ParentEntity("legacy_parent", "Legacy", "+919876500010");
            legacy.setPassword(null);
            parentRepository.save(legacy);

            ParentAuthDto dto = new ParentAuthDto();
            dto.setMode("login");
            dto.setPhoneNumber("9876500010");
            dto.setPassword("AttackerPass1");

            assertThatThrownBy(() -> authService.authenticateParent(dto))
                    .isInstanceOf(ApiException.class);

            assertThat(parentRepository.findByUserId("legacy_parent"))
                    .get()
                    .extracting(ParentEntity::getPassword)
                    .isNull();
        }
    }

    // ------------------------------------------------------------ account enumeration

    @Test
    @DisplayName("an unknown account and a wrong password are indistinguishable")
    void doesNotLeakAccountExistence() {
        authService.authenticateStudent(register(PHONE, PASSWORD));

        ApiException wrongPassword = catchApi(() -> authService.authenticateStudent(login(PHONE, "Nope12345")));
        ApiException noSuchAccount = catchApi(() -> authService.authenticateStudent(login("9876511111", "Nope12345")));

        assertThat(wrongPassword.getCode()).isEqualTo(noSuchAccount.getCode());
        assertThat(wrongPassword.getMessage()).isEqualTo(noSuchAccount.getMessage());
        assertThat(wrongPassword.getStatus()).isEqualTo(noSuchAccount.getStatus());
    }

    // --------------------------------------------------------------------- validation

    @Test
    void rejectsDuplicateRegistration() {
        authService.authenticateStudent(register(PHONE, PASSWORD));

        assertThatThrownBy(() -> authService.authenticateStudent(register(PHONE, PASSWORD)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("ACCOUNT_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("a number already registered in another spelling is still a duplicate")
    void duplicateDetectionIsSpellingInsensitive() {
        authService.authenticateStudent(register("9876500021", PASSWORD));

        assertThatThrownBy(() -> authService.authenticateStudent(register("+91 98765 00021", PASSWORD)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("ACCOUNT_ALREADY_EXISTS"));
    }

    @Test
    void rejectsMalformedPhoneNumberOnRegistration() {
        assertThatThrownBy(() -> authService.authenticateStudent(register("12345", PASSWORD)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("INVALID_PHONE"));
    }

    @Test
    @DisplayName("phone numbers are stored canonically regardless of how they were typed")
    void storesCanonicalPhoneNumbers() {
        StudentEntity student = authService.authenticateStudent(register("098765 00031", PASSWORD));

        assertThat(student.getPhoneNumber()).isEqualTo("+919876500031");
    }

    @Test
    void generatesAUniqueUserIdWhenNoneRequested() {
        StudentEntity first = authService.authenticateStudent(register("9876500041", PASSWORD));
        StudentEntity second = authService.authenticateStudent(register("9876500042", PASSWORD));

        assertThat(first.getUserId()).isNotBlank().isNotEqualTo(second.getUserId());
    }

    @Test
    void rejectsAnAlreadyTakenUserId() {
        StudentAuthDto first = register("9876500051", PASSWORD);
        first.setUserId("taken_id");
        authService.authenticateStudent(first);

        StudentAuthDto second = register("9876500052", PASSWORD);
        second.setUserId("taken_id");

        assertThatThrownBy(() -> authService.authenticateStudent(second))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("USER_ID_TAKEN"));
    }

    @Test
    @DisplayName("a new student gets a report row with zeroed, not invented, activity")
    void createsPlaceholderReportWithoutFabricatedActivity() {
        StudentEntity student = authService.authenticateStudent(register(PHONE, PASSWORD));

        assertThat(parentReportRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()))
                .singleElement()
                .satisfies(report -> {
                    assertThat(report.getVerifiedStudyMinutes()).isZero();
                    assertThat(report.getOverallEri()).isZero();
                });
    }

    // ------------------------------------------------------------------------ helpers

    private StudentAuthDto register(String phone, String password) {
        StudentAuthDto dto = new StudentAuthDto();
        dto.setMode("register");
        dto.setName("Test Student");
        dto.setPhoneNumber(phone);
        dto.setParentPhoneNumber(PARENT_PHONE);
        dto.setExamTarget("NEET 2027 Repeater");
        dto.setPassword(password);
        return dto;
    }

    private StudentAuthDto login(String phone, String password) {
        StudentAuthDto dto = new StudentAuthDto();
        dto.setMode("login");
        dto.setPhoneNumber(phone);
        dto.setPassword(password);
        return dto;
    }

    private static ApiException catchApi(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected an ApiException but none was thrown");
        } catch (ApiException e) {
            return e;
        }
    }
}
