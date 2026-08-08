package com.dhruv.config;

import com.dhruv.domain.ParentEntity;
import com.dhruv.domain.ParentReportEntity;
import com.dhruv.domain.StudentEntity;
import com.dhruv.domain.TimetableEntity;
import com.dhruv.domain.TimetableSlotEntity;
import com.dhruv.repository.ParentReportRepository;
import com.dhruv.repository.ParentRepository;
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
    private final ParentRepository parentRepository;
    private final TimetableRepository timetableRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final ParentReportRepository parentReportRepository;

    public DatabaseSeeder(StudentRepository studentRepository, 
                          ParentRepository parentRepository,
                          TimetableRepository timetableRepository, 
                          TimetableSlotRepository timetableSlotRepository, 
                          ParentReportRepository parentReportRepository) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.timetableRepository = timetableRepository;
        this.timetableSlotRepository = timetableSlotRepository;
        this.parentReportRepository = parentReportRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed Parent: Rajesh Sharma
        Optional<ParentEntity> existingParent = parentRepository.findByPhoneNumber("+919876543211");
        if (existingParent.isEmpty()) {
            ParentEntity parent = new ParentEntity("parent_rajesh", "Rajesh Sharma", "+919876543211");
            parentRepository.save(parent);
        }

        // Seed Student 1: Aarav Sharma (NEET 2027 Repeater)
        Optional<StudentEntity> existingAarav = studentRepository.findByPhoneNumber("+919876543210");
        if (existingAarav.isEmpty()) {
            StudentEntity aarav = new StudentEntity("aarav_2027", "+919876543210", "+919876543211", "Aarav Sharma", "NEET 2027 Repeater");
            aarav.setLevel(12);
            aarav.setXp(3450);
            aarav.setStreakCount(47);
            aarav.setFreezeBufferCount(1);
            StudentEntity savedAarav = studentRepository.save(aarav);

            // Timetable for Aarav
            TimetableEntity timetable = new TimetableEntity(savedAarav.getId(), "Standard Daily Schedule", "Mon,Tue,Wed,Thu,Fri");
            TimetableEntity savedTimetable = timetableRepository.save(timetable);

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

            // Parent Report for Aarav
            ParentReportEntity parentReportAarav = new ParentReportEntity(
                savedAarav.getId(),
                "Aarav Sharma",
                "NEET 2027 Repeater",
                74.5,
                480,
                "Consistent & High Effort",
                "Showed up 6 out of 7 days; completed 140 verified PYQs in Physics & Chemistry.",
                "This week, ask Aarav about his Chemistry revision. He logged 8 hours of verified practice.",
                "Don't ask about his mock test raw score; he is actively reviewing errors with the Error DNA tool."
            );
            parentReportRepository.save(parentReportAarav);
        }

        // Seed Student 2: Ananya Sharma (JEE Advanced 2027)
        Optional<StudentEntity> existingAnanya = studentRepository.findByPhoneNumber("+919876543222");
        if (existingAnanya.isEmpty()) {
            StudentEntity ananya = new StudentEntity("ananya_2027", "+919876543222", "+919876543211", "Ananya Sharma", "JEE Advanced 2027");
            ananya.setLevel(14);
            ananya.setXp(4200);
            ananya.setStreakCount(52);
            ananya.setFreezeBufferCount(2);
            StudentEntity savedAnanya = studentRepository.save(ananya);

            // Parent Report for Ananya
            ParentReportEntity parentReportAnanya = new ParentReportEntity(
                savedAnanya.getId(),
                "Ananya Sharma",
                "JEE Advanced 2027",
                81.2,
                520,
                "Top Performer (Exceeding Goal)",
                "Mastered Rotational Mechanics & Integral Calculus; solved 185 Advanced PYQs with 84% accuracy.",
                "Praise Ananya for solving 185 Advanced Level Mathematics problems cleanly this week.",
                "Avoid comparing her weekly schedule with standard school hours; her self-paced blocks are working."
            );
            parentReportRepository.save(parentReportAnanya);
        }
    }
}
