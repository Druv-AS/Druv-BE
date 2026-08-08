package com.dhruv.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public class ParentReportDto {
    private UUID studentId;
    private String studentUserId;
    private String studentName;
    private String examTarget;
    private double overallEri = 72.4;
    private int verifiedStudyMinutes;
    private String effortRating; // High, Consistent, Recovering
    private String weeklyWin;
    private String supportAsk;
    private String scriptWhatToSay;
    private String scriptWhatNotToSay;
    private boolean isSentToParent;
    private String sentAt;

    public ParentReportDto() {}

    public ParentReportDto(UUID studentId, String studentUserId, String studentName, String examTarget, double overallEri, int verifiedStudyMinutes, String effortRating, String weeklyWin, String supportAsk, String scriptWhatToSay, String scriptWhatNotToSay, boolean isSentToParent, String sentAt) {
        this.studentId = studentId;
        this.studentUserId = studentUserId;
        this.studentName = studentName;
        this.examTarget = examTarget;
        this.overallEri = overallEri;
        this.verifiedStudyMinutes = verifiedStudyMinutes;
        this.effortRating = effortRating;
        this.weeklyWin = weeklyWin;
        this.supportAsk = supportAsk;
        this.scriptWhatToSay = scriptWhatToSay;
        this.scriptWhatNotToSay = scriptWhatNotToSay;
        this.isSentToParent = isSentToParent;
        this.sentAt = sentAt;
    }

    public ParentReportDto(String studentName, String examTarget, int verifiedStudyMinutes, String effortRating, String weeklyWin, String supportAsk, String scriptWhatToSay, String scriptWhatNotToSay) {
        this.studentName = studentName;
        this.examTarget = examTarget;
        this.verifiedStudyMinutes = verifiedStudyMinutes;
        this.effortRating = effortRating;
        this.weeklyWin = weeklyWin;
        this.supportAsk = supportAsk;
        this.scriptWhatToSay = scriptWhatToSay;
        this.scriptWhatNotToSay = scriptWhatNotToSay;
    }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public String getStudentUserId() { return studentUserId; }
    public void setStudentUserId(String studentUserId) { this.studentUserId = studentUserId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getExamTarget() { return examTarget; }
    public void setExamTarget(String examTarget) { this.examTarget = examTarget; }

    public double getOverallEri() { return overallEri; }
    public void setOverallEri(double overallEri) { this.overallEri = overallEri; }

    public int getVerifiedStudyMinutes() { return verifiedStudyMinutes; }
    public void setVerifiedStudyMinutes(int verifiedStudyMinutes) { this.verifiedStudyMinutes = verifiedStudyMinutes; }

    public String getEffortRating() { return effortRating; }
    public void setEffortRating(String effortRating) { this.effortRating = effortRating; }

    public String getWeeklyWin() { return weeklyWin; }
    public void setWeeklyWin(String weeklyWin) { this.weeklyWin = weeklyWin; }

    public String getSupportAsk() { return supportAsk; }
    public void setSupportAsk(String supportAsk) { this.supportAsk = supportAsk; }

    public String getScriptWhatToSay() { return scriptWhatToSay; }
    public void setScriptWhatToSay(String scriptWhatToSay) { this.scriptWhatToSay = scriptWhatToSay; }

    public String getScriptWhatNotToSay() { return scriptWhatNotToSay; }
    public void setScriptWhatNotToSay(String scriptWhatNotToSay) { this.scriptWhatNotToSay = scriptWhatNotToSay; }

    public boolean isSentToParent() { return isSentToParent; }
    public void setSentToParent(boolean sentToParent) { isSentToParent = sentToParent; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }
}
