package com.prismite.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class InstanceStore {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path home = Path.of(System.getProperty("user.home"), ".prismite");
    private final Path profilesFile = home.resolve("instances.json");
    private final Path instancesRoot = home.resolve("instances");

    public synchronized List<InstanceProfile> list() throws IOException {
        Files.createDirectories(home);
        if (!Files.exists(profilesFile)) return new ArrayList<>();
        return mapper.readValue(profilesFile.toFile(), new TypeReference<>() {});
    }

    public synchronized InstanceProfile create(String name, String gameVersion, String loader) throws IOException {
        String id = UUID.randomUUID().toString();
        Path gameDir = instancesRoot.resolve(id).resolve(".minecraft");
        Files.createDirectories(gameDir.resolve("mods"));
        Files.createDirectories(gameDir.resolve("config"));
        InstanceProfile profile = new InstanceProfile(id, name, gameVersion, loader, gameDir.toString());
        List<InstanceProfile> all = list();
        all.add(profile);
        save(all);
        return profile;
    }

    public synchronized InstanceProfile importDirectory(String name, String gameVersion, String loader, Path gameDir) throws IOException {
        if (!Files.isDirectory(gameDir)) throw new IOException("Selected path is not a directory");
        String id = UUID.randomUUID().toString();
        Files.createDirectories(gameDir.resolve("mods"));
        InstanceProfile profile = new InstanceProfile(id, name, gameVersion, loader, gameDir.toAbsolutePath().normalize().toString());
        List<InstanceProfile> all = list();
        all.add(profile);
        save(all);
        return profile;
    }

    public synchronized void save(List<InstanceProfile> profiles) throws IOException {
        Files.createDirectories(home);
        mapper.writerWithDefaultPrettyPrinter().writeValue(profilesFile.toFile(), profiles);
    }

    public Path modsDirectory(InstanceProfile profile) throws IOException {
        Path dir = Path.of(profile.gameDirectory()).toAbsolutePath().normalize().resolve("mods");
        Files.createDirectories(dir);
        return dir;
    }

    public record InstanceProfile(String id, String name, String gameVersion, String loader, String gameDirectory) {
        @Override public String toString() { return name + "  ·  Minecraft " + gameVersion + "  ·  " + loader; }
    }
}
