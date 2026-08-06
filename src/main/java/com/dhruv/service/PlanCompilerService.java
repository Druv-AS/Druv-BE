package com.dhruv.service;

import com.dhruv.dto.PlanBlockDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlanCompilerService {

    public List<PlanBlockDto> generateDailyTimetable() {
        List<PlanBlockDto> blocks = new ArrayList<>();

        // Morning Block - Organic Chemistry (High Decay / Highest Leverage)
        blocks.add(new PlanBlockDto(
                "B01",
                "06:30 AM - 08:00 AM",
                "Chemistry",
                "Organic Reaction Mechanisms (Decay Halt)",
                "RETRIEVAL_PRACTICE",
                90,
                8.0,
                true
        ));

        // Mid-Morning Block - Physics Thermodynamics
        blocks.add(new PlanBlockDto(
                "B02",
                "09:30 AM - 11:30 AM",
                "Physics",
                "Thermodynamics & Heat (Weak Spot Clearing)",
                "NEW_CONCEPT",
                120,
                5.2,
                false
        ));

        // Afternoon Block - Biology Genetics
        blocks.add(new PlanBlockDto(
                "B03",
                "02:00 PM - 04:00 PM",
                "Biology",
                "Genetics & Inheritance (High Weightage Practice)",
                "RETRIEVAL_PRACTICE",
                120,
                11.5,
                false
        ));

        // Evening Block - Timed Mock Practice & Silent Co-Study
        blocks.add(new PlanBlockDto(
                "B04",
                "06:00 PM - 07:30 PM",
                "Physics & Chem",
                "Timed 20-Q Set + Silent Co-Study Block",
                "TIMED_MOCK",
                90,
                6.5,
                false
        ));

        // Night Block - Day Closeout & Tomorrow's First Block Confirmation
        blocks.add(new PlanBlockDto(
                "B05",
                "09:30 PM - 10:00 PM",
                "General",
                "Evening Closeout & Tomorrow Commitment Check",
                "CLOSEOUT",
                30,
                0.0,
                false
        ));

        return blocks;
    }
}
