package com.dhruv.config;

import com.dhruv.domain.ParentEntity;
import com.dhruv.domain.ParentReportEntity;
import com.dhruv.domain.StudentEntity;
import com.dhruv.domain.TimetableEntity;
import com.dhruv.domain.TimetableSlotEntity;
import com.dhruv.repository.ParentReportRepository;
import com.dhruv.repository.ParentRepository;
import com.dhruv.repository.StudentRepository;
import com.dhruv.repository.TimetableRepository;
import com.dhruv.repository.TimetableSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Demo data for local development.
 *
 * <p>Restricted to the {@code dev} profile. This previously ran on every startup including
 * production, inserting two accounts whose passwords are published in this source file —
 * anyone reading the repository could sign in as them. Passwords are now hashed like any
 * other account, and the profile guard keeps the rows out of a real database entirely.
 */
@Component
@Profile("dev")
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    /** Development-only credentials. Never valid against a production database. */
    private static final String DEMO_STUDENT_PASSWORD = "DevDemo#2027";
    private static final String DEMO_PARENT_PASSWORD = "DevParent#2027";

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final TimetableRepository timetableRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final ParentReportRepository parentReportRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(StudentRepository studentRepository,
                          ParentRepository parentRepository,
                          TimetableRepository timetableRepository,
                          TimetableSlotRepository timetableSlotRepository,
                          ParentReportRepository parentReportRepository,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.timetableRepository = timetableRepository;
        this.timetableSlotRepository = timetableSlotRepository;
        this.parentReportRepository = parentReportRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedParent();
        seedAarav();
        seedAnanya();
        log.info("Dev seed data ready. Student: +919876543210 / {} — Parent: +919876543211 / {}",
                DEMO_STUDENT_PASSWORD, DEMO_PARENT_PASSWORD);
    }

    private void seedParent() {
        if (parentRepository.findByPhoneNumber("+919876543211").isPresent()) {
            return;
        }
        ParentEntity parent = new ParentEntity("parent_rajesh", "Rajesh Sharma", "+919876543211");
        parent.setPassword(passwordEncoder.encode(DEMO_PARENT_PASSWORD));
        parentRepository.save(parent);
    }

    private void seedAarav() {
        if (studentRepository.findByPhoneNumber("+919876543210").isPresent()) {
            return;
        }
        StudentEntity aarav = new StudentEntity(
                "aarav_2027", "+919876543210", "+919876543211", "Aarav Sharma", "NEET 2027 Repeater");
        aarav.setPassword(passwordEncoder.encode(DEMO_STUDENT_PASSWORD));
        aarav.setLevel(12);
        aarav.setXp(3450);
        aarav.setStreakCount(47);
        aarav.setFreezeBufferCount(1);
        StudentEntity saved = studentRepository.save(aarav);

        TimetableEntity timetable = timetableRepository.save(
                new TimetableEntity(saved.getId(), "Standard Daily Schedule", "Mon,Tue,Wed,Thu,Fri"));

        timetableSlotRepository.saveAll(List.of(
                new TimetableSlotEntity(timetable.getId(), "6:00 AM", "Wake up & Freshen up", "—", 1),
                new TimetableSlotEntity(timetable.getId(), "6:20 AM – 7:30 AM", "Revise previous day's lectures", "15 Retrieval MCQs", 2),
                new TimetableSlotEntity(timetable.getId(), "9:00 AM – 3:00 PM", "Coaching / Institute Lectures", "Class Notes & Drills", 3),
                new TimetableSlotEntity(timetable.getId(), "3:45 PM – 5:45 PM", "Solve today's physics & chem MCQs", "40 PYQs", 4),
                new TimetableSlotEntity(timetable.getId(), "6:15 PM – 8:15 PM", "Study concepts & Clear backlog", "30 Concept MCQs", 5),
                new TimetableSlotEntity(timetable.getId(), "9:00 PM – 10:30 PM", "Revision + Timed PYQs", "25 Timed PYQs", 6),
                new TimetableSlotEntity(timetable.getId(), "10:30 PM – 11:45 PM", "Biology NCERT Line-by-Line", "35 NCERT MCQs", 7)));

        parentReportRepository.save(new ParentReportEntity(
                saved.getId(), "Aarav Sharma", "NEET 2027 Repeater", 74.5, 480,
                "Consistent & High Effort",
                "Showed up 6 out of 7 days; completed 140 verified PYQs in Physics & Chemistry.",
                "This week, ask Aarav about his Chemistry revision. He logged 8 hours of verified practice.",
                "Don't ask about his mock test raw score; he is actively reviewing errors with the Error DNA tool."));
    }

    private void seedAnanya() {
        if (studentRepository.findByPhoneNumber("+919876543222").isPresent()) {
            return;
        }
        StudentEntity ananya = new StudentEntity(
                "ananya_2027", "+919876543222", "+919876543211", "Ananya Sharma", "JEE Advanced 2027");
        ananya.setPassword(passwordEncoder.encode(DEMO_STUDENT_PASSWORD));
        ananya.setLevel(14);
        ananya.setXp(4200);
        ananya.setStreakCount(52);
        ananya.setFreezeBufferCount(2);
        StudentEntity saved = studentRepository.save(ananya);

        parentReportRepository.save(new ParentReportEntity(
                saved.getId(), "Ananya Sharma", "JEE Advanced 2027", 81.2, 520,
                "Top Performer (Exceeding Goal)",
                "Mastered Rotational Mechanics & Integral Calculus; solved 185 Advanced PYQs with 84% accuracy.",
                "Praise Ananya for solving 185 Advanced Level Mathematics problems cleanly this week.",
                "Avoid comparing her weekly schedule with standard school hours; her self-paced blocks are working."));
    }
}
