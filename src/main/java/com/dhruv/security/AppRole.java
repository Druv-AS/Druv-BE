package com.dhruv.security;

/**
 * The two account types in the system. Previously this lived only in React state,
 * which meant a user could grant themselves the PARENT role by editing localStorage.
 */
public enum AppRole {
    STUDENT,
    PARENT;

    /** Spring Security convention: authorities carry the {@code ROLE_} prefix. */
    public String authority() {
        return "ROLE_" + name();
    }
}
