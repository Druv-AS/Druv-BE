package com.dhruv.dto;

public class ParentReportDto {
    private String studentName;
    private String examTarget;
    private int verifiedStudyMinutes;
    private String effortRating; // High, Consistent, Recovering
    private String weeklyWin;
    private String supportAsk;
    private String scriptWhatToSay;
    private String scriptWhatNotToSay;

    public ParentReportDto() {}

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

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getExamTarget() { return examTarget; }
    public void setExamTarget(String examTarget) { this.examTarget = examTarget; }

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
}
