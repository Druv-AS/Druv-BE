package com.dhruv.dto;

public class ConceptTileDto {
    private String id;
    private String subject; // Physics, Chemistry, Biology/Math
    private String name;
    private double weightagePercent; // e.g. 4.5%
    private double decayAdjustedMastery; // 0 - 100
    private String status; // STABLE, DECAYING, WEAK, UNATTEMPTED
    private int questionsAvailable;

    public ConceptTileDto() {}

    public ConceptTileDto(String id, String subject, String name, double weightagePercent, double decayAdjustedMastery, String status, int questionsAvailable) {
        this.id = id;
        this.subject = subject;
        this.name = name;
        this.weightagePercent = weightagePercent;
        this.decayAdjustedMastery = decayAdjustedMastery;
        this.status = status;
        this.questionsAvailable = questionsAvailable;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getWeightagePercent() { return weightagePercent; }
    public void setWeightagePercent(double weightagePercent) { this.weightagePercent = weightagePercent; }

    public double getDecayAdjustedMastery() { return decayAdjustedMastery; }
    public void setDecayAdjustedMastery(double decayAdjustedMastery) { this.decayAdjustedMastery = decayAdjustedMastery; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getQuestionsAvailable() { return questionsAvailable; }
    public void setQuestionsAvailable(int questionsAvailable) { this.questionsAvailable = questionsAvailable; }
}
