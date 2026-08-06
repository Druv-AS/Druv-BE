package com.dhruv.domain;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "students")
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "target_course", nullable = false)
    private String targetCourse;

    @Column(name = "level")
    private Integer level = 12;

    @Column(name = "xp")
    private Integer xp = 3450;

    @Column(name = "streak_count")
    private Integer streakCount = 47;

    @Column(name = "freeze_buffer_count")
    private Integer freezeBufferCount = 1;

    @Column(name = "created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public StudentEntity() {}

    public StudentEntity(String phoneNumber, String name, String targetCourse) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.targetCourse = targetCourse;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTargetCourse() { return targetCourse; }
    public void setTargetCourse(String targetCourse) { this.targetCourse = targetCourse; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getXp() { return xp; }
    public void setXp(Integer xp) { this.xp = xp; }

    public Integer getStreakCount() { return streakCount; }
    public void setStreakCount(Integer streakCount) { this.streakCount = streakCount; }

    public Integer getFreezeBufferCount() { return freezeBufferCount; }
    public void setFreezeBufferCount(Integer freezeBufferCount) { this.freezeBufferCount = freezeBufferCount; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
