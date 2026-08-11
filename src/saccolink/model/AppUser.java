package saccolink.model;

/** Mirrors one row of APP_USER (application login, not a DB account). */
public class AppUser {
    private Long userId;
    private String username;
    private String role;         // "MEMBER" or "SACCO"
    private Long memberId;       // null for SACCO
    private String displayName;

    public AppUser() {
    }

    public AppUser(Long userId, String username, String role,
                   Long memberId, String displayName) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.memberId = memberId;
        this.displayName = displayName;
    }

    public boolean isSacco() {
        return "SACCO".equalsIgnoreCase(role);
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    @Override
    public String toString() {
        return displayName == null ? username : displayName;
    }
}
