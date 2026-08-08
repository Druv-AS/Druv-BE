package com.dhruv.service;

import com.dhruv.domain.ParentEntity;
import com.dhruv.domain.ParentReportEntity;
import com.dhruv.domain.StudentEntity;
import com.dhruv.dto.ParentAuthDto;
import com.dhruv.dto.StudentAuthDto;
import com.dhruv.repository.ParentReportRepository;
import com.dhruv.repository.ParentRepository;
import com.dhruv.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final ParentReportRepository parentReportRepository;

    public AuthService(StudentRepository studentRepository, ParentRepository parentRepository, ParentReportRepository parentReportRepository) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.parentReportRepository = parentReportRepository;
    }

    @Transactional
    public StudentEntity authenticateStudent(StudentAuthDto dto) {
        String searchKey = dto.getPhoneNumber() != null ? dto.getPhoneNumber().trim() : "";
        Optional<StudentEntity> existing = studentRepository.findByPhoneNumber(searchKey);
        if (existing.isEmpty()) {
            existing = studentRepository.findByUserId(searchKey);
        }
        if (existing.isEmpty() && dto.getUserId() != null && !dto.getUserId().isBlank()) {
            existing = studentRepository.findByUserId(dto.getUserId().trim());
        }

        boolean isLoginMode = "login".equalsIgnoreCase(dto.getMode());

        if (isLoginMode) {
            if (existing.isEmpty()) {
                throw new IllegalArgumentException("ACCOUNT_NOT_FOUND: Account does not exist with this Mobile Number or User ID. Please check your input or switch to Create Account.");
            }
            StudentEntity student = existing.get();
            if (dto.getPassword() == null || dto.getPassword().isBlank()) {
                throw new IllegalArgumentException("INVALID_PASSWORD: Password is required to log in.");
            }
            if (student.getPassword() != null && !student.getPassword().isBlank() && !student.getPassword().equals(dto.getPassword())) {
                throw new IllegalArgumentException("INVALID_PASSWORD: Incorrect password. Please check your password and try again.");
            }
            if (student.getPassword() == null || student.getPassword().isBlank()) {
                student.setPassword(dto.getPassword());
                student = studentRepository.save(student);
            }
            return student;
        } else {
            // Register Mode
            StudentEntity student;
            if (existing.isPresent()) {
                student = existing.get();
                if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                    student.setPassword(dto.getPassword());
                }
                if (dto.getName() != null && !dto.getName().isBlank()) student.setName(dto.getName());
                if (dto.getUserId() != null && !dto.getUserId().isBlank()) student.setUserId(dto.getUserId());
                if (dto.getParentPhoneNumber() != null && !dto.getParentPhoneNumber().isBlank()) student.setParentPhoneNumber(dto.getParentPhoneNumber());
                if (dto.getExamTarget() != null && !dto.getExamTarget().isBlank()) student.setTargetCourse(dto.getExamTarget());
                student = studentRepository.save(student);
            } else {
                student = new StudentEntity(
                        dto.getUserId() != null && !dto.getUserId().isBlank() ? dto.getUserId() : dto.getName().toLowerCase().replaceAll("\\s+", "_"),
                        dto.getPhoneNumber(),
                        dto.getParentPhoneNumber(),
                        dto.getName(),
                        dto.getExamTarget() != null ? dto.getExamTarget() : "NEET 2027 Repeater"
                );
                if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                    student.setPassword(dto.getPassword());
                }
                student = studentRepository.save(student);
            }

            // Ensure student has a parent report record created
            List<ParentReportEntity> reports = parentReportRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
            if (reports.isEmpty()) {
                ParentReportEntity report = new ParentReportEntity(
                        student.getId(),
                        student.getName(),
                        student.getTargetCourse(),
                        74.5,
                        480,
                        "Consistent & High Effort",
                        "Showed up 6 out of 7 days; completed 140 verified PYQs in Physics & Chemistry.",
                        "This week, ask " + student.getName() + " about their revision. They logged 8 hours of verified practice.",
                        "Don't ask about mock test raw score; student is actively reviewing error patterns with the Error DNA tool."
                );
                parentReportRepository.save(report);
            }

            return student;
        }
    }

    @Transactional
    public ParentEntity authenticateParent(ParentAuthDto dto) {
        String searchKey = dto.getPhoneNumber() != null ? dto.getPhoneNumber().trim() : "";
        Optional<ParentEntity> existing = parentRepository.findByPhoneNumber(searchKey);
        if (existing.isEmpty()) {
            existing = parentRepository.findByUserId(searchKey);
        }
        if (existing.isEmpty() && dto.getUserId() != null && !dto.getUserId().isBlank()) {
            existing = parentRepository.findByUserId(dto.getUserId().trim());
        }

        boolean isLoginMode = "login".equalsIgnoreCase(dto.getMode());

        if (isLoginMode) {
            if (existing.isEmpty()) {
                throw new IllegalArgumentException("ACCOUNT_NOT_FOUND: Account does not exist with this Mobile Number or User ID. Please check your input or switch to Create Account.");
            }
            ParentEntity parent = existing.get();
            if (dto.getPassword() == null || dto.getPassword().isBlank()) {
                throw new IllegalArgumentException("INVALID_PASSWORD: Password is required to log in.");
            }
            if (parent.getPassword() != null && !parent.getPassword().isBlank() && !parent.getPassword().equals(dto.getPassword())) {
                throw new IllegalArgumentException("INVALID_PASSWORD: Incorrect password. Please check your password and try again.");
            }
            if (parent.getPassword() == null || parent.getPassword().isBlank()) {
                parent.setPassword(dto.getPassword());
                parent = parentRepository.save(parent);
            }
            return parent;
        } else {
            // Register Mode
            if (existing.isPresent()) {
                ParentEntity parent = existing.get();
                if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                    parent.setPassword(dto.getPassword());
                }
                if (dto.getName() != null && !dto.getName().isBlank()) parent.setName(dto.getName());
                if (dto.getUserId() != null && !dto.getUserId().isBlank()) parent.setUserId(dto.getUserId());
                return parentRepository.save(parent);
            } else {
                ParentEntity parent = new ParentEntity(
                        dto.getUserId() != null && !dto.getUserId().isBlank() ? dto.getUserId() : "parent_" + dto.getName().toLowerCase().replaceAll("\\s+", "_"),
                        dto.getName(),
                        dto.getPhoneNumber()
                );
                if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                    parent.setPassword(dto.getPassword());
                }
                return parentRepository.save(parent);
            }
        }
    }
}
