package com.dhruv.service;

import com.dhruv.domain.ParentReportEntity;
import com.dhruv.domain.StudentEntity;
import com.dhruv.dto.*;
import com.dhruv.repository.ParentReportRepository;
import com.dhruv.repository.StudentRepository;
import com.dhruv.util.PhoneNumbers;
import com.dhruv.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Readiness figures and parent reporting.
 *
 * <p>Authorization note: every method takes the subject's identity from the caller's
 * session rather than a request parameter, and the parent-facing methods additionally
 * verify the parent/student link. The previous implementation trusted a phone number
 * supplied by the client.
 *
 * <p><b>Known limitation.</b> {@link #getEriBreakdown}, {@link #getSyllabusHeatmap} and
 * {@link #getBacklogDebt} still return a fixed reference model rather than figures derived
 * from the student's activity — every student sees identical numbers. Producing real values
 * requires an activity-ingestion and decay model that does not exist yet. The student id is
 * taken and validated now so that the API contract and the authorization checks do not have
 * to change when those computations land.
 */
@Service
public class ReadinessLedgerService {

    private static final Logger log = LoggerFactory.getLogger(ReadinessLedgerService.class);

    private static final DateTimeFormatter SENT_AT_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ERI weighting: coverage 20%, mastery 30%, retention 20%, exam skill 15%, consistency 15%.
    private static final double W_COVERAGE = 0.20;
    private static final double W_MASTERY = 0.30;
    private static final double W_RETENTION = 0.20;
    private static final double W_EXAM_SKILL = 0.15;
    private static final double W_CONSISTENCY = 0.15;

    private final StudentRepository studentRepository;
    private final ParentReportRepository parentReportRepository;

    public ReadinessLedgerService(StudentRepository studentRepository,
                                  ParentReportRepository parentReportRepository) {
        this.studentRepository = studentRepository;
        this.parentReportRepository = parentReportRepository;
    }

    // ------------------------------------------------------------- readiness views

    public EriBreakdownDto getEriBreakdown(UUID studentId) {
        requireStudent(studentId);

        double coverage = 68.5;
        double mastery = 74.0;
        double retention = 62.0;
        double examSkill = 70.5;
        double consistency = 88.0;

        double overallEri = (coverage * W_COVERAGE)
                + (mastery * W_MASTERY)
                + (retention * W_RETENTION)
                + (examSkill * W_EXAM_SKILL)
                + (consistency * W_CONSISTENCY);

        return new EriBreakdownDto(
                Math.round(overallEri * 10.0) / 10.0,
                2.4,
                coverage, mastery, retention, examSkill, consistency,
                "Solve 20 timed Organic Chemistry PYQs to halt decay in Reaction Mechanisms.",
                "Your consistency is driving ERI growth (+2.4 points). Retention in Organic Chemistry needs revision today.");
    }

    public List<ConceptTileDto> getSyllabusHeatmap(UUID studentId) {
        requireStudent(studentId);
        return List.of(
                new ConceptTileDto("P01", "Physics", "Thermodynamics & Heat", 5.2, 45.0, "WEAK", 140),
                new ConceptTileDto("P02", "Physics", "Rotational Motion", 4.8, 52.0, "DECAYING", 110),
                new ConceptTileDto("P03", "Physics", "Current Electricity", 6.0, 84.0, "STABLE", 210),
                new ConceptTileDto("P04", "Physics", "Optics & Ray Optics", 7.1, 78.0, "STABLE", 250),
                new ConceptTileDto("P05", "Physics", "Modern Physics", 6.5, 91.0, "STABLE", 180),
                new ConceptTileDto("C01", "Chemistry", "Organic Reaction Mechanisms", 8.0, 38.0, "WEAK", 320),
                new ConceptTileDto("C02", "Chemistry", "Chemical Equilibrium", 4.5, 62.0, "DECAYING", 130),
                new ConceptTileDto("C03", "Chemistry", "Coordination Compounds", 5.8, 88.0, "STABLE", 190),
                new ConceptTileDto("C04", "Chemistry", "Electrochemistry", 4.2, 75.0, "STABLE", 150),
                new ConceptTileDto("C05", "Chemistry", "Biomolecules & Polymers", 3.0, 95.0, "STABLE", 90),
                new ConceptTileDto("B01", "Biology", "Genetics & Inheritance", 11.5, 82.0, "STABLE", 450),
                new ConceptTileDto("B02", "Biology", "Human Physiology", 12.0, 79.0, "STABLE", 510),
                new ConceptTileDto("B03", "Biology", "Plant Physiology", 7.5, 58.0, "DECAYING", 280),
                new ConceptTileDto("B04", "Biology", "Ecology & Environment", 6.0, 90.0, "STABLE", 220));
    }

    public BacklogDebtDto getBacklogDebt(UUID studentId) {
        requireStudent(studentId);
        return new BacklogDebtDto(
                6.5, 4, 1.2,
                List.of("Semiconductor Devices (2% weightage - saves 3.5 study hours)",
                        "Surface Chemistry (1.5% weightage - saves 2.0 study hours)"),
                "Repayment Plan: 45 extra focus minutes daily over 10 days. "
                        + "Low-yield topics forgiven with stated value trade-off.");
    }

    // --------------------------------------------------------------- parent reports

    /** The signed-in student's own latest report. */
    public ParentReportDto getReportForStudent(UUID studentId) {
        StudentEntity student = requireStudent(studentId);
        return latestReport(student)
                .map(report -> toDto(student, report))
                .orElseGet(() -> placeholderDto(student));
    }

    /**
     * Reports for every student who nominated this parent's number.
     *
     * @param parentPhone the authenticated parent's number, never a client-supplied value
     */
    public List<ParentReportDto> getReportsForParent(String parentPhone) {
        List<StudentEntity> students =
                studentRepository.findByParentPhoneNumberIn(PhoneNumbers.lookupVariants(parentPhone));

        List<ParentReportDto> reports = new ArrayList<>(students.size());
        for (StudentEntity student : students) {
            reports.add(latestReport(student)
                    .map(report -> toDto(student, report))
                    .orElseGet(() -> placeholderDto(student)));
        }
        return reports;
    }

    /**
     * Confirms a parent/student link that the student initiated.
     *
     * <p>The student must already list this parent's mobile number, set when they
     * registered. Previously this method wrote the caller-supplied phone number onto any
     * student found by user id or phone, which let any caller take ownership of any
     * student's reports.
     */
    @Transactional
    public void linkStudentToParent(String parentPhone, String studentIdentifier) {
        StudentEntity student = findByIdentifier(studentIdentifier)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND",
                        "No student found with that user ID or mobile number."));

        String claimedParent = PhoneNumbers.canonical(parentPhone);
        String nominatedParent = PhoneNumbers.canonical(student.getParentPhoneNumber());

        if (nominatedParent.isBlank() || !nominatedParent.equals(claimedParent)) {
            log.warn("Rejected link attempt: parent {} is not nominated by student {}",
                    claimedParent, student.getUserId());
            throw ApiException.forbidden("LINK_NOT_AUTHORISED",
                    "This student has not listed your mobile number as their parent contact. "
                            + "Ask them to add it in their profile, then try again.");
        }

        // Normalise the stored value so later comparisons hit the canonical form directly.
        if (!nominatedParent.equals(student.getParentPhoneNumber())) {
            student.setParentPhoneNumber(nominatedParent);
            studentRepository.save(student);
        }
        log.info("Linked student {} to parent {}", student.getUserId(), claimedParent);
    }

    /**
     * Sets the parent mobile number the signed-in student nominates as their contact.
     *
     * <p>This is the student's half of the parent link: {@link #linkStudentToParent} only
     * succeeds for a parent whose number appears here. Editing it was previously a local
     * change in the browser that was never sent to the server, so it had no effect at all.
     *
     * @return the canonical stored number
     */
    @Transactional
    public String updateParentContact(UUID studentId, String parentPhone) {
        StudentEntity student = requireStudent(studentId);

        if (parentPhone == null || parentPhone.isBlank()) {
            student.setParentPhoneNumber(null);
            studentRepository.save(student);
            return "";
        }
        if (!PhoneNumbers.isValid(parentPhone)) {
            throw ApiException.badRequest("INVALID_PARENT_PHONE",
                    "Enter a valid 10-digit Indian mobile number.");
        }

        String canonical = PhoneNumbers.canonical(parentPhone);
        student.setParentPhoneNumber(canonical);
        studentRepository.save(student);
        log.info("Student {} updated their parent contact", student.getUserId());
        return canonical;
    }

    /** Marks the signed-in student's latest report as released to their parent. */
    @Transactional
    public ParentReportDto sendStudentReport(UUID studentId) {
        StudentEntity student = requireStudent(studentId);

        ParentReportEntity report = latestReport(student).orElseGet(() -> new ParentReportEntity(
                student.getId(),
                student.getName(),
                student.getTargetCourse(),
                0.0,
                0,
                "Not enough data yet",
                "No activity recorded yet.",
                "Ask " + student.getName() + " how their week on Dhruv is going.",
                "Avoid comparing progress to other students this early."));

        report.setIsSentToParent(true);
        report.setSentAt(ZonedDateTime.now());
        return toDto(student, parentReportRepository.save(report));
    }

    // ---------------------------------------------------------------------- helpers

    private StudentEntity requireStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Student record not found."));
    }

    private Optional<ParentReportEntity> latestReport(StudentEntity student) {
        List<ParentReportEntity> reports =
                parentReportRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        return reports.isEmpty() ? Optional.empty() : Optional.of(reports.get(0));
    }

    private Optional<StudentEntity> findByIdentifier(String identifier) {
        Optional<StudentEntity> byUserId = studentRepository.findByUserId(identifier.trim());
        if (byUserId.isPresent()) {
            return byUserId;
        }
        for (String variant : PhoneNumbers.lookupVariants(identifier)) {
            Optional<StudentEntity> match = studentRepository.findByPhoneNumber(variant);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private ParentReportDto toDto(StudentEntity student, ParentReportEntity report) {
        return new ParentReportDto(
                student.getId(),
                student.getUserId(),
                student.getName(),
                student.getTargetCourse(),
                report.getOverallEri() != null ? report.getOverallEri() : 0.0,
                report.getVerifiedStudyMinutes(),
                report.getEffortRating(),
                report.getWeeklyWin(),
                "Focus on " + student.getTargetCourse() + " core subjects",
                report.getScriptWhatToSay(),
                report.getScriptWhatNotToSay(),
                Boolean.TRUE.equals(report.getIsSentToParent()),
                report.getSentAt() != null ? report.getSentAt().format(SENT_AT_FORMAT) : null);
    }

    /**
     * Shown before any report has been generated. Reports zeroes rather than the invented
     * "74.5 ERI / 480 minutes" the old code returned, which presented fabricated study
     * activity to parents as though it were measured.
     */
    private ParentReportDto placeholderDto(StudentEntity student) {
        return new ParentReportDto(
                student.getId(),
                student.getUserId(),
                student.getName(),
                student.getTargetCourse(),
                0.0,
                0,
                "Not enough data yet",
                "No verified activity recorded yet.",
                "Focus on " + student.getTargetCourse(),
                "Ask " + student.getName() + " how their first week on Dhruv is going.",
                "Avoid comparing progress to other students this early.",
                false,
                null);
    }
}
