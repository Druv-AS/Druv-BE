package com.dhruv.controller;

import com.dhruv.dto.*;
import com.dhruv.security.AppUserPrincipal;
import com.dhruv.service.PlanCompilerService;
import com.dhruv.service.ReadinessLedgerService;
import com.dhruv.websocket.CoStudyWebSocketHandler;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Readiness, planning, and parent-portal endpoints.
 *
 * <p>Every handler that touches per-user data derives the subject from
 * {@link AuthenticationPrincipal}. The previous signatures accepted {@code parentPhone} and
 * {@code studentPhoneOrId} as request parameters with permissive defaults, so any
 * unauthenticated caller could read or mutate any account's records by guessing a phone
 * number.
 */
@RestController
@RequestMapping("/api/v1")
public class ReadinessController {

    private final ReadinessLedgerService readinessLedgerService;
    private final CoStudyWebSocketHandler coStudyWebSocketHandler;
    private final PlanCompilerService planCompilerService;

    public ReadinessController(ReadinessLedgerService readinessLedgerService,
                               CoStudyWebSocketHandler coStudyWebSocketHandler,
                               PlanCompilerService planCompilerService) {
        this.readinessLedgerService = readinessLedgerService;
        this.coStudyWebSocketHandler = coStudyWebSocketHandler;
        this.planCompilerService = planCompilerService;
    }

    @GetMapping("/readiness/eri")
    public ResponseEntity<EriBreakdownDto> getEriBreakdown(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(readinessLedgerService.getEriBreakdown(principal.getId()));
    }

    @GetMapping("/readiness/heatmap")
    public ResponseEntity<List<ConceptTileDto>> getSyllabusHeatmap(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(readinessLedgerService.getSyllabusHeatmap(principal.getId()));
    }

    @GetMapping("/readiness/backlog-debt")
    public ResponseEntity<BacklogDebtDto> getBacklogDebt(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(readinessLedgerService.getBacklogDebt(principal.getId()));
    }

    /** The signed-in student's own report. */
    @GetMapping("/readiness/parent-report")
    public ResponseEntity<ParentReportDto> getOwnReport(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(readinessLedgerService.getReportForStudent(principal.getId()));
    }

    /** Reports for children linked to the signed-in parent. Restricted to ROLE_PARENT. */
    @GetMapping("/parent/students")
    public ResponseEntity<List<ParentReportDto>> getParentStudents(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(readinessLedgerService.getReportsForParent(principal.getPhoneNumber()));
    }

    /**
     * Links a student to the calling parent.
     *
     * <p>Succeeds only when the student nominated this parent's mobile number during
     * registration. Without that check a parent account could attach any student in the
     * system and read their reports.
     */
    @PostMapping("/parent/link-student")
    public ResponseEntity<Map<String, Object>> linkStudentToParent(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody LinkStudentRequestDto req) {

        readinessLedgerService.linkStudentToParent(principal.getPhoneNumber(), req.getStudentIdentifier());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Student linked to your parent portal."));
    }

    /**
     * Sets the parent mobile number the signed-in student nominates as their contact.
     * Only that number may then link to this student from the parent portal.
     */
    @PutMapping("/student/parent-contact")
    public ResponseEntity<Map<String, Object>> updateParentContact(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody ParentContactRequestDto req) {

        String stored = readinessLedgerService.updateParentContact(
                principal.getId(), req.getParentPhoneNumber());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "parentPhoneNumber", stored,
                "message", stored.isEmpty()
                        ? "Parent contact removed."
                        : "Parent contact saved. Your parent can now link to your account."));
    }

    /** Releases the signed-in student's latest report to their linked parent. */
    @PostMapping("/student/send-report")
    public ResponseEntity<ParentReportDto> sendOwnReport(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(readinessLedgerService.sendStudentReport(principal.getId()));
    }

    @GetMapping("/costudy/room-state")
    public ResponseEntity<CoStudyRoomStateDto> getCoStudyRoomState() {
        return ResponseEntity.ok(coStudyWebSocketHandler.currentRoomState());
    }

    @GetMapping("/plan/daily")
    public ResponseEntity<List<PlanBlockDto>> getDailyPlan(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(planCompilerService.generateDailyTimetable(principal.getId()));
    }
}
