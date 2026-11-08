package com.prismite.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipFile;

public final class ModMetadataReader {
    public String describe(Path file) {
        String name = file.getFileName().toString();
        try (ZipFile zip = new ZipFile(file.toFile())) {
            if (zip.getEntry("fabric.mod.json") != null) return name + "  ·  Fabric mod";
            if (zip.getEntry("META-INF/mods.toml") != null) return name + "  ·  Forge/NeoForge mod";
            if (zip.getEntry("mcmod.info") != null) return name + "  ·  legacy Forge mod";
            return name + "  ·  metadata unavailable (possibly a library/resource pack)";
        } catch (IOException ex) {
            return name + "  ·  unreadable archive";
        }
    }
}
