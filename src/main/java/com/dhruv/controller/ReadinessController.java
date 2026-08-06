package com.dhruv.controller;

import com.dhruv.dto.*;
import com.dhruv.service.PlanCompilerService;
import com.dhruv.service.ReadinessLedgerService;
import com.dhruv.websocket.CoStudyWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReadinessController {

    private final ReadinessLedgerService readinessLedgerService;
    private final CoStudyWebSocketHandler coStudyWebSocketHandler;
    private final com.dhruv.service.PlanCompilerService planCompilerService;

    public ReadinessController(ReadinessLedgerService readinessLedgerService, CoStudyWebSocketHandler coStudyWebSocketHandler, com.dhruv.service.PlanCompilerService planCompilerService) {
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
