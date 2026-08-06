package com.dhruv.domain;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "parent_weekly_reports")
public class ParentReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "verified_study_minutes", nullable = false)
    private Integer verifiedStudyMinutes;

    @Column(name = "effort_rating", nullable = false)
    private String effortRating;

    @Column(name = "weekly_win", nullable = false, columnDefinition = "TEXT")
    private String weeklyWin;

    @Column(name = "script_what_to_say", nullable = false, columnDefinition = "TEXT")
    private String scriptWhatToSay;

    @Column(name = "script_what_not_to_say", nullable = false, columnDefinition = "TEXT")
    private String scriptWhatNotToSay;

    @Column(name = "is_approved")
    private Boolean isApproved = true;

    @Column(name = "created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public ParentReportEntity() {}

    public ParentReportEntity(UUID studentId, Integer verifiedStudyMinutes, String effortRating, String weeklyWin, String scriptWhatToSay, String scriptWhatNotToSay) {
        this.studentId = studentId;
        this.verifiedStudyMinutes = verifiedStudyMinutes;
        this.effortRating = effortRating;
        this.weeklyWin = weeklyWin;
        this.scriptWhatToSay = scriptWhatToSay;
        this.scriptWhatNotToSay = scriptWhatNotToSay;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public Integer getVerifiedStudyMinutes() { return verifiedStudyMinutes; }
    public void setVerifiedStudyMinutes(Integer verifiedStudyMinutes) { this.verifiedStudyMinutes = verifiedStudyMinutes; }

    public String getEffortRating() { return effortRating; }
    public void setEffortRating(String effortRating) { this.effortRating = effortRating; }

    public String getWeeklyWin() { return weeklyWin; }
    public void setWeeklyWin(String weeklyWin) { this.weeklyWin = weeklyWin; }

    public String getScriptWhatToSay() { return scriptWhatToSay; }
    public void setScriptWhatToSay(String scriptWhatToSay) { this.scriptWhatToSay = scriptWhatToSay; }

    public String getScriptWhatNotToSay() { return scriptWhatNotToSay; }
    public void setScriptWhatNotToSay(String scriptWhatNotToSay) { this.scriptWhatNotToSay = scriptWhatNotToSay; }

    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
