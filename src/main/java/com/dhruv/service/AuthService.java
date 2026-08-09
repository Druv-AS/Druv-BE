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
        String rawPhone = dto.getPhoneNumber() != null ? dto.getPhoneNumber().trim() : "";
        String cleanPhone = rawPhone.replaceAll("[^0-9]", "");
        if (cleanPhone.startsWith("91") && cleanPhone.length() == 12) {
            cleanPhone = cleanPhone.substring(2);
        }

        Optional<StudentEntity> existing = studentRepository.findByPhoneNumber(rawPhone);
        if (existing.isEmpty() && !cleanPhone.isEmpty()) {
            existing = studentRepository.findByPhoneNumber(cleanPhone);
        }
        if (existing.isEmpty() && !cleanPhone.isEmpty()) {
            existing = studentRepository.findByPhoneNumber("+91" + cleanPhone);
        }
        if (existing.isEmpty() && !rawPhone.isEmpty()) {
            existing = studentRepository.findByUserId(rawPhone);
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
            }
            if (dto.getExamTarget() != null && !dto.getExamTarget().isBlank()) {
                student.setTargetCourse(dto.getExamTarget());
            }
            student = studentRepository.save(student);
            return student;
        } else {
            // Register Mode
            if (existing.isPresent()) {
                throw new IllegalArgumentException("ACCOUNT_ALREADY_EXISTS: An account already exists with this Mobile Number or User ID. Please switch to Login.");
            }

            // Generate unique userId
            String requestedUserId = (dto.getUserId() != null && !dto.getUserId().isBlank()) ? dto.getUserId().trim() : null;
            String finalUserId;
            if (requestedUserId != null) {
                if (studentRepository.findByUserId(requestedUserId).isPresent()) {
                    throw new IllegalArgumentException("ACCOUNT_ALREADY_EXISTS: User ID '" + requestedUserId + "' is already taken. Please choose a different User ID.");
                }
                finalUserId = requestedUserId;
            } else {
                String baseUserId = dto.getName() != null && !dto.getName().isBlank() 
                        ? dto.getName().toLowerCase().replaceAll("\\s+", "_") 
                        : "student";
                String suffix = cleanPhone.length() >= 4 ? cleanPhone.substring(cleanPhone.length() - 4) : String.valueOf((int)(Math.random() * 9000 + 1000));
                finalUserId = baseUserId + "_" + suffix;
                int counter = 1;
                while (studentRepository.findByUserId(finalUserId).isPresent()) {
                    finalUserId = baseUserId + "_" + suffix + "_" + counter++;
                }
            }

            StudentEntity student = new StudentEntity(
                    finalUserId,
                    dto.getPhoneNumber() != null ? dto.getPhoneNumber().trim() : "",
                    dto.getParentPhoneNumber() != null ? dto.getParentPhoneNumber().trim() : "",
                    dto.getName() != null ? dto.getName().trim() : "Student",
                    dto.getExamTarget() != null ? dto.getExamTarget() : "NEET 2027 Repeater"
            );
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                student.setPassword(dto.getPassword());
            }
            student = studentRepository.save(student);

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
        String rawPhone = dto.getPhoneNumber() != null ? dto.getPhoneNumber().trim() : "";
        String cleanPhone = rawPhone.replaceAll("[^0-9]", "");
        if (cleanPhone.startsWith("91") && cleanPhone.length() == 12) {
            cleanPhone = cleanPhone.substring(2);
        }

        Optional<ParentEntity> existing = parentRepository.findByPhoneNumber(rawPhone);
        if (existing.isEmpty() && !cleanPhone.isEmpty()) {
            existing = parentRepository.findByPhoneNumber(cleanPhone);
        }
        if (existing.isEmpty() && !cleanPhone.isEmpty()) {
            existing = parentRepository.findByPhoneNumber("+91" + cleanPhone);
        }
        if (existing.isEmpty() && !rawPhone.isEmpty()) {
            existing = parentRepository.findByUserId(rawPhone);
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
                throw new IllegalArgumentException("ACCOUNT_ALREADY_EXISTS: An account already exists with this Mobile Number or User ID. Please switch to Login.");
            }

            String requestedUserId = (dto.getUserId() != null && !dto.getUserId().isBlank()) ? dto.getUserId().trim() : null;
            String finalUserId;
            if (requestedUserId != null) {
                if (parentRepository.findByUserId(requestedUserId).isPresent()) {
                    throw new IllegalArgumentException("ACCOUNT_ALREADY_EXISTS: Parent User ID '" + requestedUserId + "' is already taken. Please choose a different User ID.");
                }
                finalUserId = requestedUserId;
            } else {
                String baseUserId = dto.getName() != null && !dto.getName().isBlank() 
                        ? "parent_" + dto.getName().toLowerCase().replaceAll("\\s+", "_") 
                        : "parent";
                String suffix = cleanPhone.length() >= 4 ? cleanPhone.substring(cleanPhone.length() - 4) : String.valueOf((int)(Math.random() * 9000 + 1000));
                finalUserId = baseUserId + "_" + suffix;
                int counter = 1;
                while (parentRepository.findByUserId(finalUserId).isPresent()) {
                    finalUserId = baseUserId + "_" + suffix + "_" + counter++;
                }
            }

            ParentEntity parent = new ParentEntity(
                    finalUserId,
                    dto.getName() != null ? dto.getName().trim() : "Parent",
                    dto.getPhoneNumber() != null ? dto.getPhoneNumber().trim() : ""
            );
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                parent.setPassword(dto.getPassword());
            }
            return parentRepository.save(parent);
        }
    }
}
