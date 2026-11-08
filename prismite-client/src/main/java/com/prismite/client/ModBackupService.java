package com.prismite.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ModBackupService {
    public void backup(Path modsDirectory, Path archive) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            if (!Files.isDirectory(modsDirectory)) return;
            try (var files = Files.list(modsDirectory)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String name = file.getFileName().toString().toLowerCase();
                    if (name.endsWith(".jar") || name.endsWith(".zip")) {
                        zip.putNextEntry(new ZipEntry("mods/" + file.getFileName()));
                        Files.copy(file, zip);
                        zip.closeEntry();
                    }
                }
            }
        }
    }

    public int restore(Path archive, Path modsDirectory) throws IOException {
        Files.createDirectories(modsDirectory);
        int restored = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || !name.startsWith("mods/")) continue;
                String filename = name.substring("mods/".length());
                if (filename.isBlank() || filename.contains("/") || filename.contains("..")) continue;
                String lower = filename.toLowerCase();
                if (!(lower.endsWith(".jar") || lower.endsWith(".zip"))) continue;
                Path target = modsDirectory.resolve(filename).normalize();
                if (!target.getParent().equals(modsDirectory.toAbsolutePath().normalize())) continue;
                Files.copy(zip, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                restored++;
                zip.closeEntry();
            }
        }
        return restored;
    }

    public void clearMods(Path modsDirectory) throws IOException {
        if (!Files.isDirectory(modsDirectory)) return;
        try (var files = Files.list(modsDirectory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString().toLowerCase();
                if (name.endsWith(".jar") || name.endsWith(".zip")) Files.deleteIfExists(file);
            }
        }
    }
}
