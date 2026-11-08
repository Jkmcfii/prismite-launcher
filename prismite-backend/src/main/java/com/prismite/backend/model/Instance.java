package com.prismite.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "instances")
public class Instance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Column
    private String loader = "vanilla";

    @Column
    private String gameDirectory;

    @Column
    private String javaPath;

    @Column
    private String memory = "4G";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Instance() {}

    public Instance(User user, String name, String version, String loader) {
        this.user = user;
        this.name = name;
        this.version = version;
        this.loader = loader;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getLoader() { return loader; }
    public void setLoader(String loader) { this.loader = loader; }

    public String getGameDirectory() { return gameDirectory; }
    public void setGameDirectory(String gameDirectory) { this.gameDirectory = gameDirectory; }

    public String getJavaPath() { return javaPath; }
    public void setJavaPath(String javaPath) { this.javaPath = javaPath; }

    public String getMemory() { return memory; }
    public void setMemory(String memory) { this.memory = memory; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
