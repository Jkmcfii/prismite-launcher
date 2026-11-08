package com.prismite.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private boolean microsoftLinked = false;

    @Column(nullable = false)
    private boolean googleLinked = false;

    @Column(nullable = false)
    private boolean discordLinked = false;

    @Column(nullable = false)
    private boolean minecraftOwned = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant lastLoginAt;

    @Column
    private String microsoftUserId;

    @Column
    private String googleUserId;

    @Column
    private String discordUserId;

    public User() {}

    public User(String email, String username, String passwordHash) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isMicrosoftLinked() { return microsoftLinked; }
    public void setMicrosoftLinked(boolean microsoftLinked) { this.microsoftLinked = microsoftLinked; }

    public boolean isGoogleLinked() { return googleLinked; }
    public void setGoogleLinked(boolean googleLinked) { this.googleLinked = googleLinked; }

    public boolean isDiscordLinked() { return discordLinked; }
    public void setDiscordLinked(boolean discordLinked) { this.discordLinked = discordLinked; }

    public boolean isMinecraftOwned() { return minecraftOwned; }
    public void setMinecraftOwned(boolean minecraftOwned) { this.minecraftOwned = minecraftOwned; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getMicrosoftUserId() { return microsoftUserId; }
    public void setMicrosoftUserId(String microsoftUserId) { this.microsoftUserId = microsoftUserId; }

    public String getGoogleUserId() { return googleUserId; }
    public void setGoogleUserId(String googleUserId) { this.googleUserId = googleUserId; }

    public String getDiscordUserId() { return discordUserId; }
    public void setDiscordUserId(String discordUserId) { this.discordUserId = discordUserId; }
}
