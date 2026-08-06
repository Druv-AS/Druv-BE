package com.dhruv.dto;

public class PlanBlockDto {
    private String id;
    private String timeSlot; // e.g. "06:30 AM - 08:00 AM"
    private String subject; // Physics, Chemistry, Biology
    private String topicName;
    private String activityType; // RETRIEVAL_PRACTICE, NEW_CONCEPT, REVISION
    private int durationMinutes;
    private double weightagePercent;
    private boolean isCompleted;

    public PlanBlockDto() {}

    public PlanBlockDto(String id, String timeSlot, String subject, String topicName, String activityType, int durationMinutes, double weightagePercent, boolean isCompleted) {
        this.id = id;
        this.timeSlot = timeSlot;
        this.subject = subject;
        this.topicName = topicName;
        this.activityType = activityType;
        this.durationMinutes = durationMinutes;
        this.weightagePercent = weightagePercent;
        this.isCompleted = isCompleted;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public double getWeightagePercent() { return weightagePercent; }
    public void setWeightagePercent(double weightagePercent) { this.weightagePercent = weightagePercent; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
