package com.dhruv.dto;

import com.dhruv.domain.ParentEntity;
import com.dhruv.domain.StudentEntity;
import com.dhruv.security.AppRole;

import java.util.UUID;

/**
 * What the client is told about the signed-in account.
 *
 * <p>The auth endpoints used to serialise the JPA entity directly, which coupled the wire
 * format to the schema and meant any column added later would be published by default.
 * This DTO is an explicit allowlist.
 *
 * @param csrfToken the token the SPA must echo in {@code X-XSRF-TOKEN} on mutations; it is
 *                  delivered in the body because a cross-origin frontend cannot read the
 *                  XSRF-TOKEN cookie from JavaScript
 */
public record AuthenticatedUserDto(
        UUID id,
        String userId,
        String name,
        String phoneNumber,
        String parentPhoneNumber,
        String targetCourse,
        AppRole role,
        Integer level,
        Integer xp,
        Integer streakCount,
        Integer freezeBufferCount,
        String csrfToken) {

    public static AuthenticatedUserDto of(StudentEntity student, String csrfToken) {
        return new AuthenticatedUserDto(
                student.getId(),
                student.getUserId(),
                student.getName(),
                student.getPhoneNumber(),
                student.getParentPhoneNumber(),
                student.getTargetCourse(),
                AppRole.STUDENT,
                student.getLevel(),
                student.getXp(),
                student.getStreakCount(),
                student.getFreezeBufferCount(),
                csrfToken);
    }

    public static AuthenticatedUserDto of(ParentEntity parent, String csrfToken) {
        return new AuthenticatedUserDto(
                parent.getId(),
                parent.getUserId(),
                parent.getName(),
                parent.getPhoneNumber(),
                null,
                null,
                AppRole.PARENT,
                null, null, null, null,
                csrfToken);
    }
}
