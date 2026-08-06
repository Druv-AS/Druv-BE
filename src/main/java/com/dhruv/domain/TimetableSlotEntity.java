package com.dhruv.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "timetable_slots")
public class TimetableSlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "timetable_id", nullable = false)
    private UUID timetableId;

    @Column(name = "time_slot", nullable = false)
    private String timeSlot;

    @Column(name = "activity_name", nullable = false, columnDefinition = "TEXT")
    private String activityName;

    @Column(name = "target_mcq_count")
    private String targetMcqCount = "—";

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public TimetableSlotEntity() {}

    public TimetableSlotEntity(UUID timetableId, String timeSlot, String activityName, String targetMcqCount, Integer displayOrder) {
        this.timetableId = timetableId;
        this.timeSlot = timeSlot;
        this.activityName = activityName;
        this.targetMcqCount = targetMcqCount;
        this.displayOrder = displayOrder;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTimetableId() { return timetableId; }
    public void setTimetableId(UUID timetableId) { this.timetableId = timetableId; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public String getTargetMcqCount() { return targetMcqCount; }
    public void setTargetMcqCount(String targetMcqCount) { this.targetMcqCount = targetMcqCount; }

    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
