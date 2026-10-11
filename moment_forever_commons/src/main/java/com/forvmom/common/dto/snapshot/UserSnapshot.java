package com.forvmom.common.dto.snapshot;

public class UserSnapshot {
    private Long userId;
    private String email;
    private String fullName;
    /**
     * Caller username (gateway JWT subject) captured at booking time.
     * {@code null} for snapshots cached before this field existed.
     */
    private String username;

    public UserSnapshot() {
    }

    public UserSnapshot(Long userId, String email, String fullName) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
    }

    public UserSnapshot(Long userId, String email, String fullName, String username) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
