package com.dhruv.dto;

import java.util.List;

public class BacklogDebtDto {
    private double debtHours;
    private int missedTopicsCount;
    private double interestAccruedHours; // Decay penalty hours
    private List<String> proposedForgivenessTopics;
    private String repaymentPlanSummary;

    public BacklogDebtDto() {}

    public BacklogDebtDto(double debtHours, int missedTopicsCount, double interestAccruedHours, List<String> proposedForgivenessTopics, String repaymentPlanSummary) {
        this.debtHours = debtHours;
        this.missedTopicsCount = missedTopicsCount;
        this.interestAccruedHours = interestAccruedHours;
        this.proposedForgivenessTopics = proposedForgivenessTopics;
        this.repaymentPlanSummary = repaymentPlanSummary;
    }

    public double getDebtHours() { return debtHours; }
    public void setDebtHours(double debtHours) { this.debtHours = debtHours; }

    public int getMissedTopicsCount() { return missedTopicsCount; }
    public void setMissedTopicsCount(int missedTopicsCount) { this.missedTopicsCount = missedTopicsCount; }

    public double getInterestAccruedHours() { return interestAccruedHours; }
    public void setInterestAccruedHours(double interestAccruedHours) { this.interestAccruedHours = interestAccruedHours; }

    public List<String> getProposedForgivenessTopics() { return proposedForgivenessTopics; }
    public void setProposedForgivenessTopics(List<String> proposedForgivenessTopics) { this.proposedForgivenessTopics = proposedForgivenessTopics; }

    public String getRepaymentPlanSummary() { return repaymentPlanSummary; }
    public void setRepaymentPlanSummary(String repaymentPlanSummary) { this.repaymentPlanSummary = repaymentPlanSummary; }
}
