package com.dhruv.controller;

import com.dhruv.domain.ParentEntity;
import com.dhruv.domain.StudentEntity;
import com.dhruv.dto.ParentAuthDto;
import com.dhruv.dto.StudentAuthDto;
import com.dhruv.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/student")
    public ResponseEntity<?> authenticateStudent(@RequestBody StudentAuthDto dto) {
        try {
            StudentEntity student = authService.authenticateStudent(dto);
            return ResponseEntity.ok(student);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/parent")
    public ResponseEntity<?> authenticateParent(@RequestBody ParentAuthDto dto) {
        try {
            ParentEntity parent = authService.authenticateParent(dto);
            return ResponseEntity.ok(parent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
