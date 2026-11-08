package com.prismite.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ModController {

    @GetMapping("/mods")
    public List<Map<String, Object>> searchMods(@RequestParam(required = false, defaultValue = "") String query) {
        String q = query == null ? "" : query.toLowerCase();

        return List.of(
                Map.of(
                        "id", "fabric-api",
                        "name", "Fabric API",
                        "description", "Core library for Fabric mods",
                        "version", "0.92.1",
                        "downloads", 123456,
                        "category", "library"
                ),
                Map.of(
                        "id", "sodium",
                        "name", "Sodium",
                        "description", "High performance renderer for Fabric",
                        "version", "mc1.20.4",
                        "downloads", 421000,
                        "category", "rendering"
                ),
                Map.of(
                        "id", "modmenu",
                        "name", "Mod Menu",
                        "description", "In-game mod configuration menu",
                        "version", "11.0.1",
                        "downloads", 310000,
                        "category", "utility"
                )
        ).stream()
                .filter(item -> q.isEmpty() || item.get("name").toString().toLowerCase().contains(q) || item.get("description").toString().toLowerCase().contains(q))
                .toList();
    }
}
