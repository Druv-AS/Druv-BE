package com.dhruv.controller;

import com.dhruv.dto.*;
import com.dhruv.service.PlanCompilerService;
import com.dhruv.service.ReadinessLedgerService;
import com.dhruv.websocket.CoStudyWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ReadinessController {

    private final ReadinessLedgerService readinessLedgerService;
    private final CoStudyWebSocketHandler coStudyWebSocketHandler;
    private final PlanCompilerService planCompilerService;

    public ReadinessController(ReadinessLedgerService readinessLedgerService, CoStudyWebSocketHandler coStudyWebSocketHandler, PlanCompilerService planCompilerService) {
        this.readinessLedgerService = readinessLedgerService;
        this.coStudyWebSocketHandler = coStudyWebSocketHandler;
        this.planCompilerService = planCompilerService;
    }

    @GetMapping("/readiness/eri")
    public ResponseEntity<EriBreakdownDto> getEriBreakdown() {
        return ResponseEntity.ok(readinessLedgerService.getEriBreakdown());
    }

    @GetMapping("/readiness/heatmap")
    public ResponseEntity<List<ConceptTileDto>> getSyllabusHeatmap() {
        return ResponseEntity.ok(readinessLedgerService.getSyllabusHeatmap());
    }

    @GetMapping("/readiness/backlog-debt")
    public ResponseEntity<BacklogDebtDto> getBacklogDebt() {
        return ResponseEntity.ok(readinessLedgerService.getBacklogDebt());
    }

    @GetMapping("/readiness/parent-report")
    public ResponseEntity<ParentReportDto> getParentReport() {
        return ResponseEntity.ok(readinessLedgerService.getParentReport());
    }

    @GetMapping("/parent/students")
    public ResponseEntity<List<ParentReportDto>> getParentStudents(@RequestParam(value = "parentPhone", required = false, defaultValue = "+919876543211") String parentPhone) {
        return ResponseEntity.ok(readinessLedgerService.getReportsForParent(parentPhone));
    }

    @PostMapping("/parent/link-student")
    public ResponseEntity<Map<String, Object>> linkStudentToParent(@RequestBody LinkStudentRequestDto req) {
        boolean success = readinessLedgerService.linkStudentToParent(req.getParentPhoneNumber(), req.getStudentIdentifier());
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Student successfully linked to parent portal"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student not found with User ID or Phone Number: " + req.getStudentIdentifier()));
        }
    }

    @PostMapping("/student/send-report")
    public ResponseEntity<ParentReportDto> sendStudentReport(@RequestParam(value = "studentPhoneOrId", required = false, defaultValue = "+919876543210") String studentPhoneOrId) {
        ParentReportDto report = readinessLedgerService.sendStudentReport(studentPhoneOrId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/costudy/room-state")
    public ResponseEntity<CoStudyRoomStateDto> getCoStudyRoomState() {
        CoStudyRoomStateDto state = new CoStudyRoomStateDto(
                "NEET-REPEATERS-ROOM-1",
                coStudyWebSocketHandler.getActiveCount(),
                2400, // 40 minutes remaining in current 50 min block
                true,
                "FOCUS_50MIN"
        );
        return ResponseEntity.ok(state);
    }

    @GetMapping("/plan/daily")
    public ResponseEntity<List<PlanBlockDto>> getDailyPlan() {
        return ResponseEntity.ok(planCompilerService.generateDailyTimetable());
    }
}
