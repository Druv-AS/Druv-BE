package com.dhruv.dto;

public class EriBreakdownDto {
    private double overallEri; // 0 - 100
    private double deltaWeekly; // +2.4, -1.1 etc.
    private double coverage; // 20% weight
    private double mastery; // 30% weight
    private double retention; // 20% weight
    private double examSkill; // 15% weight
    private double consistency; // 15% weight
    private String topLeverageAction;
    private String statusMessage;

    public EriBreakdownDto() {}

    public EriBreakdownDto(double overallEri, double deltaWeekly, double coverage, double mastery, 
                           double retention, double examSkill, double consistency, 
                           String topLeverageAction, String statusMessage) {
        this.overallEri = overallEri;
        this.deltaWeekly = deltaWeekly;
        this.coverage = coverage;
        this.mastery = mastery;
        this.retention = retention;
        this.examSkill = examSkill;
        this.consistency = consistency;
        this.topLeverageAction = topLeverageAction;
        this.statusMessage = statusMessage;
    }

    public double getOverallEri() { return overallEri; }
    public void setOverallEri(double overallEri) { this.overallEri = overallEri; }

    public double getDeltaWeekly() { return deltaWeekly; }
    public void setDeltaWeekly(double deltaWeekly) { this.deltaWeekly = deltaWeekly; }

    public double getCoverage() { return coverage; }
    public void setCoverage(double coverage) { this.coverage = coverage; }

    public double getMastery() { return mastery; }
    public void setMastery(double mastery) { this.mastery = mastery; }

    public double getRetention() { return retention; }
    public void setRetention(double retention) { this.retention = retention; }

    public double getExamSkill() { return examSkill; }
    public void setExamSkill(double examSkill) { this.examSkill = examSkill; }

    public double getConsistency() { return consistency; }
    public void setConsistency(double consistency) { this.consistency = consistency; }

    public String getTopLeverageAction() { return topLeverageAction; }
    public void setTopLeverageAction(String topLeverageAction) { this.topLeverageAction = topLeverageAction; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
}
