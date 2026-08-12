package com.dhruv.service;

import com.dhruv.dto.PlanBlockDto;
import com.dhruv.repository.StudentRepository;
import com.dhruv.web.ApiException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Builds the daily study plan.
 *
 * <p><b>Known limitation.</b> The plan is a fixed reference schedule shared by every
 * student; it is not compiled from the student's own weak concepts, decay curve, or
 * timetable. The student id is validated here so the endpoint is properly scoped and
 * authorised today, and so the signature does not change when real compilation lands.
 */
@Service
public class PlanCompilerService {

    private final StudentRepository studentRepository;

    public PlanCompilerService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<PlanBlockDto> generateDailyTimetable(UUID studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw ApiException.notFound("STUDENT_NOT_FOUND", "Student record not found.");
        }

        return List.of(
                // Highest-leverage slot first: the concept with the steepest decay.
                new PlanBlockDto("B01", "06:30 AM - 08:00 AM", "Chemistry",
                        "Organic Reaction Mechanisms (Decay Halt)", "RETRIEVAL_PRACTICE", 90, 8.0, true),
                new PlanBlockDto("B02", "09:30 AM - 11:30 AM", "Physics",
                        "Thermodynamics & Heat (Weak Spot Clearing)", "NEW_CONCEPT", 120, 5.2, false),
                new PlanBlockDto("B03", "02:00 PM - 04:00 PM", "Biology",
                        "Genetics & Inheritance (High Weightage Practice)", "RETRIEVAL_PRACTICE", 120, 11.5, false),
                new PlanBlockDto("B04", "06:00 PM - 07:30 PM", "Physics & Chem",
                        "Timed 20-Q Set + Silent Co-Study Block", "TIMED_MOCK", 90, 6.5, false),
                new PlanBlockDto("B05", "09:30 PM - 10:00 PM", "General",
                        "Evening Closeout & Tomorrow Commitment Check", "CLOSEOUT", 30, 0.0, false));
    }
}
