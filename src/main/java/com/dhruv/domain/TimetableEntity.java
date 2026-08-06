package com.dhruv.domain;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "timetables")
public class TimetableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active_days", nullable = false)
    private String activeDays = "Mon,Tue,Wed,Thu,Fri";

    @Column(name = "created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public TimetableEntity() {}

    public TimetableEntity(UUID studentId, String name, String activeDays) {
        this.studentId = studentId;
        this.name = name;
        this.activeDays = activeDays;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getActiveDays() { return activeDays; }
    public void setActiveDays(String activeDays) { this.activeDays = activeDays; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
