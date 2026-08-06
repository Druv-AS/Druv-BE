package com.dhruv.config;

import com.dhruv.domain.ParentReportEntity;
import com.dhruv.domain.StudentEntity;
import com.dhruv.domain.TimetableEntity;
import com.dhruv.domain.TimetableSlotEntity;
import com.dhruv.repository.ParentReportRepository;
import com.dhruv.repository.StudentRepository;
import com.dhruv.repository.TimetableSlotRepository;
import com.dhruv.repository.TimetableRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final TimetableRepository timetableRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final ParentReportRepository parentReportRepository;

    public DatabaseSeeder(StudentRepository studentRepository, 
                          TimetableRepository timetableRepository, 
                          TimetableSlotRepository timetableSlotRepository, 
                          ParentReportRepository parentReportRepository) {
        this.studentRepository = studentRepository;
        this.timetableRepository = timetableRepository;
        this.timetableSlotRepository = timetableSlotRepository;
        this.parentReportRepository = parentReportRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Automatically check if demo student exists; if not, create student and initial tables/data
        Optional<StudentEntity> existing = studentRepository.findByPhoneNumber("+919876543210");
        
        if (existing.isEmpty()) {
            StudentEntity student = new StudentEntity("+919876543210", "Aarav Sharma", "NEET 2027 Repeater");
            student.setLevel(12);
            student.setXp(3450);
            student.setStreakCount(47);
            student.setFreezeBufferCount(1);
            StudentEntity savedStudent = studentRepository.save(student);

            // Auto-create initial Timetable for student
            TimetableEntity timetable = new TimetableEntity(savedStudent.getId(), "Standard Daily Schedule", "Mon,Tue,Wed,Thu,Fri");
            TimetableEntity savedTimetable = timetableRepository.save(timetable);

            // Auto-create Timetable Slots
            List<TimetableSlotEntity> slots = List.of(
                new TimetableSlotEntity(savedTimetable.getId(), "6:00 AM", "Wake up & Freshen up", "—", 1),
                new TimetableSlotEntity(savedTimetable.getId(), "6:20 AM – 7:30 AM", "Revise previous day's lectures", "15 Retrieval MCQs", 2),
                new TimetableSlotEntity(savedTimetable.getId(), "9:00 AM – 3:00 PM", "Coaching / Institute Lectures", "Class Notes & Drills", 3),
                new TimetableSlotEntity(savedTimetable.getId(), "3:45 PM – 5:45 PM", "Solve today's physics & chem MCQs", "40 PYQs", 4),
                new TimetableSlotEntity(savedTimetable.getId(), "6:15 PM – 8:15 PM", "Study concepts & Clear backlog", "30 Concept MCQs", 5),
                new TimetableSlotEntity(savedTimetable.getId(), "9:00 PM – 10:30 PM", "Revision + Timed PYQs", "25 Timed PYQs", 6),
                new TimetableSlotEntity(savedTimetable.getId(), "10:30 PM – 11:45 PM", "Biology NCERT Line-by-Line", "35 NCERT MCQs", 7)
            );
            timetableSlotRepository.saveAll(slots);

            // Auto-create Initial Parent Weekly Report
            ParentReportEntity parentReport = new ParentReportEntity(
                savedStudent.getId(),
                480,
                "Consistent & High Effort",
                "Showed up 6 out of 7 days; completed 140 verified PYQs in Physics & Chemistry.",
                "This week, ask Aarav about his Chemistry revision. He logged 8 hours of verified practice.",
                "Don't ask about his mock test raw score; he is actively reviewing errors with the Error DNA tool."
            );
            parentReportRepository.save(parentReport);
        }
    }
}
