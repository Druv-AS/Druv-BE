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

    @Column(name = "user_id", unique = true)
    private String userId;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(name = "parent_phone_number")
    private String parentPhoneNumber;

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

    @Column(name = "password")
    private String password;

    @Column(name = "created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public StudentEntity() {}

    public StudentEntity(String userId, String phoneNumber, String parentPhoneNumber, String name, String targetCourse) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.parentPhoneNumber = parentPhoneNumber;
        this.name = name;
        this.targetCourse = targetCourse;
    }

    public StudentEntity(String phoneNumber, String name, String targetCourse) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.targetCourse = targetCourse;
        this.userId = name.toLowerCase().replaceAll("\\s+", "_") + "_" + (phoneNumber.length() > 4 ? phoneNumber.substring(phoneNumber.length() - 4) : "1234");
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getParentPhoneNumber() { return parentPhoneNumber; }
    public void setParentPhoneNumber(String parentPhoneNumber) { this.parentPhoneNumber = parentPhoneNumber; }

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

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
