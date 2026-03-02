package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_callsign", columnList = "callsign")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt encrypted

    @Column(length = 20)
    private String callsign; // Ham radio callsign

    @Column(length = 100)
    private String fullName;

    @Column(length = 10)
    private String gridSquare;

    // Default location for user (fallback when station location not set)
    @Column(name = "default_latitude")
    private Double defaultLatitude;

    @Column(name = "default_longitude")
    private Double defaultLongitude;

    @Column(name = "default_grid", length = 8)
    private String defaultGrid;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean accountNonExpired = true;

    @Column(nullable = false)
    private Boolean accountNonLocked = true;

    @Column(nullable = false)
    private Boolean credentialsNonExpired = true;

    @Column(length = 500)
    private String qrzApiKey; // User's own QRZ API key

    @Column(name = "station_color_preferences", columnDefinition = "TEXT")
    private String stationColorPreferences; // JSON: { station1: "#hex", ..., gota: "#hex" }

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    // Enum for roles
    public enum Role {
        ROLE_USER,      // Regular user
        ROLE_ADMIN,     // System administrator
        ROLE_CREATOR,   // Can create logs (implied for all users)
        ROLE_OPERATOR   // Contest operator
    }

    // Convenience methods
    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }

    public boolean isAdmin() {
        return this.roles.contains(Role.ROLE_ADMIN);
    }
}
