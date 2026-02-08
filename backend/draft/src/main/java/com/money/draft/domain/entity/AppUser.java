package com.money.draft.domain.entity;


import com.money.draft.domain.enums.Role;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class AppUser {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=100)
    private String username;

    @Column(nullable=false, length=255)
    private String password; // TEMP: plaintext; will switch to BCrypt later

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=10)
    private Role role;

    @Column(name="account_id")
    private Long accountId; // null for ADMIN

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt = Instant.now();

    public AppUser() {}

    public AppUser(String username, String password, Role role, Long accountId) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.accountId = accountId;
    }

    // --- Getters/Setters ---
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
