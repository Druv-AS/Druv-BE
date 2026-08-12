package com.dhruv.security;

import java.io.Serializable;
import java.util.UUID;

/**
 * Identity of the authenticated caller, stored in the HTTP session.
 *
 * <p>Every authorization decision reads from this object. Request parameters are never
 * trusted to identify the caller — that was the source of the IDOR issues on the parent
 * and student report endpoints.
 *
 * <p>Serializable so the session survives a shared session store (see README).
 */
public final class AppUserPrincipal implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String userId;
    private final String phoneNumber;
    private final AppRole role;

    public AppUserPrincipal(UUID id, String userId, String phoneNumber, AppRole role) {
        this.id = id;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    public UUID getId() { return id; }

    public String getUserId() { return userId; }

    public String getPhoneNumber() { return phoneNumber; }

    public AppRole getRole() { return role; }

    /**
     * Identity is the (id, role) pair. Spring Security's SessionRegistry keys concurrent
     * sessions by principal equality, so without this the one-session-per-account limit
     * would never match two logins by the same user.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppUserPrincipal other)) return false;
        return java.util.Objects.equals(id, other.id) && role == other.role;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, role);
    }

    /** Used by Spring Security as the principal name in logs and audit trails. */
    @Override
    public String toString() {
        return userId;
    }
}
