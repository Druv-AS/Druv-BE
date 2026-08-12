package com.dhruv.service;

import com.dhruv.domain.ParentEntity;
import com.dhruv.domain.ParentReportEntity;
import com.dhruv.domain.StudentEntity;
import com.dhruv.dto.ParentAuthDto;
import com.dhruv.dto.StudentAuthDto;
import com.dhruv.repository.ParentReportRepository;
import com.dhruv.repository.ParentRepository;
import com.dhruv.repository.StudentRepository;
import com.dhruv.util.PhoneNumbers;
import com.dhruv.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

/**
 * Registration and credential verification for both account types.
 *
 * <p>Security-relevant changes from the original implementation:
 * <ul>
 *   <li>Passwords are BCrypt-hashed, never stored or compared in plaintext.</li>
 *   <li>Login no longer writes a password onto an account that has none. Previously the
 *       first caller to attempt a login against a password-less account silently claimed
 *       it, which was a complete account takeover.</li>
 *   <li>Password comparison always runs the encoder, even when no account matched, so
 *       response timing does not distinguish a missing account from a wrong password.</li>
 *   <li>Phone numbers are canonicalised on write, so the parent/student link check
 *       compares a single representation.</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * A valid BCrypt hash of a value no caller can supply. Used to burn the same CPU on a
     * miss as on a hit, so an attacker cannot enumerate accounts by measuring latency.
     */
    private static final String TIMING_DECOY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_USERID_ATTEMPTS = 50;

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final ParentReportRepository parentReportRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(StudentRepository studentRepository,
                       ParentRepository parentRepository,
                       ParentReportRepository parentReportRepository,
                       PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.parentReportRepository = parentReportRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------------------------------------------------------ student

    @Transactional
    public StudentEntity authenticateStudent(StudentAuthDto dto) {
        return isLogin(dto.getMode()) ? loginStudent(dto) : registerStudent(dto);
    }

    private StudentEntity loginStudent(StudentAuthDto dto) {
        Optional<StudentEntity> found = findStudent(dto.getPhoneNumber(), dto.getUserId());
        String storedHash = found.map(StudentEntity::getPassword).orElse(null);

        if (!verify(dto.getPassword(), storedHash)) {
            throw invalidCredentials();
        }

        StudentEntity student = found.orElseThrow(AuthService::invalidCredentials);

        // A student may retarget their exam at sign-in; this is the only field login mutates.
        if (dto.getExamTarget() != null && !dto.getExamTarget().isBlank()
                && !dto.getExamTarget().equals(student.getTargetCourse())) {
            student.setTargetCourse(dto.getExamTarget().trim());
            student = studentRepository.save(student);
        }
        return student;
    }

    private StudentEntity registerStudent(StudentAuthDto dto) {
        if (!PhoneNumbers.isValid(dto.getPhoneNumber())) {
            throw ApiException.badRequest("INVALID_PHONE",
                    "Enter a valid 10-digit Indian mobile number.");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw ApiException.badRequest("NAME_REQUIRED", "Please enter your full name.");
        }
        String parentPhone = dto.getParentPhoneNumber();
        if (parentPhone != null && !parentPhone.isBlank() && !PhoneNumbers.isValid(parentPhone)) {
            throw ApiException.badRequest("INVALID_PARENT_PHONE",
                    "Enter a valid 10-digit parent mobile number, or leave it blank.");
        }

        // Check the phone number alone. Folding the requested user id into this lookup
        // would report "account already exists" when in fact only the user id collided,
        // sending the user to the sign-in screen for an account that is not theirs.
        if (findStudentByPhone(dto.getPhoneNumber()).isPresent()) {
            throw accountExists();
        }

        String name = dto.getName().trim();
        String finalUserId = resolveUserId(dto.getUserId(), name, dto.getPhoneNumber(),
                candidate -> studentRepository.findByUserId(candidate).isPresent(), "");

        StudentEntity student = new StudentEntity(
                finalUserId,
                PhoneNumbers.canonical(dto.getPhoneNumber()),
                (parentPhone == null || parentPhone.isBlank()) ? null : PhoneNumbers.canonical(parentPhone),
                name,
                (dto.getExamTarget() == null || dto.getExamTarget().isBlank())
                        ? "NEET 2027 Repeater" : dto.getExamTarget().trim());
        student.setPassword(passwordEncoder.encode(dto.getPassword()));

        StudentEntity saved = studentRepository.save(student);
        ensureParentReport(saved);
        log.info("Registered student userId={}", saved.getUserId());
        return saved;
    }

    // ------------------------------------------------------------------- parent

    @Transactional
    public ParentEntity authenticateParent(ParentAuthDto dto) {
        return isLogin(dto.getMode()) ? loginParent(dto) : registerParent(dto);
    }

    private ParentEntity loginParent(ParentAuthDto dto) {
        Optional<ParentEntity> found = findParent(dto.getPhoneNumber(), dto.getUserId());
        String storedHash = found.map(ParentEntity::getPassword).orElse(null);

        if (!verify(dto.getPassword(), storedHash)) {
            throw invalidCredentials();
        }
        return found.orElseThrow(AuthService::invalidCredentials);
    }

    private ParentEntity registerParent(ParentAuthDto dto) {
        if (!PhoneNumbers.isValid(dto.getPhoneNumber())) {
            throw ApiException.badRequest("INVALID_PHONE",
                    "Enter a valid 10-digit Indian mobile number.");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw ApiException.badRequest("NAME_REQUIRED", "Please enter your full name.");
        }
        if (findParentByPhone(dto.getPhoneNumber()).isPresent()) {
            throw accountExists();
        }

        String name = dto.getName().trim();
        String finalUserId = resolveUserId(dto.getUserId(), name, dto.getPhoneNumber(),
                candidate -> parentRepository.findByUserId(candidate).isPresent(), "parent_");

        ParentEntity parent = new ParentEntity(
                finalUserId, name, PhoneNumbers.canonical(dto.getPhoneNumber()));
        parent.setPassword(passwordEncoder.encode(dto.getPassword()));

        ParentEntity saved = parentRepository.save(parent);
        log.info("Registered parent userId={}", saved.getUserId());
        return saved;
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Compares a candidate password against a stored hash in constant-ish time.
     *
     * <p>When no account matched, or the account predates hashing and has no usable hash,
     * the encoder still runs against a decoy so the failure path costs the same as success.
     */
    private boolean verify(String rawPassword, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            passwordEncoder.matches(rawPassword == null ? "" : rawPassword, TIMING_DECOY_HASH);
            return false;
        }
        return passwordEncoder.matches(rawPassword == null ? "" : rawPassword, storedHash);
    }

    /** Phone-number lookup only, across every historical spelling. */
    private Optional<StudentEntity> findStudentByPhone(String phone) {
        for (String variant : PhoneNumbers.lookupVariants(phone)) {
            Optional<StudentEntity> match = studentRepository.findByPhoneNumber(variant);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private Optional<ParentEntity> findParentByPhone(String phone) {
        for (String variant : PhoneNumbers.lookupVariants(phone)) {
            Optional<ParentEntity> match = parentRepository.findByPhoneNumber(variant);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private Optional<StudentEntity> findStudent(String phone, String userId) {
        Optional<StudentEntity> byPhone = findStudentByPhone(phone);
        if (byPhone.isPresent()) {
            return byPhone;
        }
        // The sign-in field accepts either a phone number or a user ID.
        Optional<StudentEntity> byTypedIdentifier = blankToEmpty(phone)
                .flatMap(studentRepository::findByUserId);
        if (byTypedIdentifier.isPresent()) {
            return byTypedIdentifier;
        }
        return blankToEmpty(userId).flatMap(studentRepository::findByUserId);
    }

    private Optional<ParentEntity> findParent(String phone, String userId) {
        Optional<ParentEntity> byPhone = findParentByPhone(phone);
        if (byPhone.isPresent()) {
            return byPhone;
        }
        Optional<ParentEntity> byTypedIdentifier = blankToEmpty(phone)
                .flatMap(parentRepository::findByUserId);
        if (byTypedIdentifier.isPresent()) {
            return byTypedIdentifier;
        }
        return blankToEmpty(userId).flatMap(parentRepository::findByUserId);
    }

    private static Optional<String> blankToEmpty(String value) {
        return (value == null || value.isBlank()) ? Optional.empty() : Optional.of(value.trim());
    }

    /**
     * Picks a free user ID. An explicit request that is already taken is an error; a
     * generated one falls back to a random suffix rather than looping forever.
     */
    private String resolveUserId(String requested, String name, String phone,
                                 java.util.function.Predicate<String> taken, String prefix) {
        if (requested != null && !requested.isBlank()) {
            String candidate = requested.trim();
            if (taken.test(candidate)) {
                throw ApiException.conflict("USER_ID_TAKEN",
                        "User ID '" + candidate + "' is already taken. Please choose another.");
            }
            return candidate;
        }

        String base = prefix + name.toLowerCase().replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.isBlank() || base.equals(prefix)) {
            base = prefix.isEmpty() ? "student" : "parent";
        }
        String nsn = PhoneNumbers.nationalDigits(phone);
        String suffix = nsn.length() >= 4 ? nsn.substring(nsn.length() - 4) : fourRandomDigits();

        String candidate = base + "_" + suffix;
        for (int i = 0; i < MAX_USERID_ATTEMPTS && taken.test(candidate); i++) {
            candidate = base + "_" + suffix + "_" + fourRandomDigits();
        }
        if (taken.test(candidate)) {
            throw ApiException.conflict("USER_ID_TAKEN",
                    "Could not allocate a user ID. Please choose one explicitly.");
        }
        return candidate;
    }

    private static String fourRandomDigits() {
        return String.valueOf(1000 + RANDOM.nextInt(9000));
    }

    /** Every new student gets a report row so the parent portal has something to show. */
    private void ensureParentReport(StudentEntity student) {
        List<ParentReportEntity> reports =
                parentReportRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        if (!reports.isEmpty()) {
            return;
        }
        parentReportRepository.save(new ParentReportEntity(
                student.getId(),
                student.getName(),
                student.getTargetCourse(),
                0.0,
                0,
                "Not enough data yet",
                "No activity recorded yet. The first weekly summary appears after a week of study.",
                "Ask " + student.getName() + " how their first week on Dhruv is going.",
                "Avoid comparing progress to other students this early."));
    }

    private static boolean isLogin(String mode) {
        return "login".equalsIgnoreCase(mode);
    }

    /**
     * One message for both "no such account" and "wrong password". Distinguishing them
     * would let an unauthenticated caller test which mobile numbers are registered.
     */
    private static ApiException invalidCredentials() {
        return ApiException.unauthorized("INVALID_CREDENTIALS",
                "Incorrect mobile number, user ID, or password.");
    }

    private static ApiException accountExists() {
        return ApiException.conflict("ACCOUNT_ALREADY_EXISTS",
                "An account already exists with this mobile number or user ID. Please sign in instead.");
    }
}
