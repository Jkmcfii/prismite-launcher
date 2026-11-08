package com.prismite.backend.controller;

import com.prismite.backend.dto.CreateInstanceRequest;
import com.prismite.backend.model.Instance;
import com.prismite.backend.service.InstanceService;
import com.prismite.backend.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InstanceController {
    private final InstanceService instanceService;
    private final JwtService jwtService;

    public InstanceController(InstanceService instanceService, JwtService jwtService) {
        this.instanceService = instanceService;
        this.jwtService = jwtService;
    }

    @GetMapping("/instances")
    public ResponseEntity<?> getInstances(@RequestHeader("Authorization") String authHeader) {
        String email = jwtService.extractEmail(authHeader.replace("Bearer ", ""));
        List<Instance> instances = instanceService.getInstancesByEmail(email);
        return ResponseEntity.ok(instances);
    }

    @PostMapping("/instances")
    public ResponseEntity<?> createInstance(@RequestHeader("Authorization") String authHeader, @RequestBody CreateInstanceRequest request) {
        String email = jwtService.extractEmail(authHeader.replace("Bearer ", ""));
        Instance created = instanceService.createInstance(
                email,
                request.getName(),
                request.getVersion(),
                request.getLoader(),
                request.getGameDirectory(),
                request.getJavaPath(),
                request.getMemory()
        );

        return ResponseEntity.ok(Map.of(
                "id", created.getId(),
                "name", created.getName(),
                "version", created.getVersion(),
                "loader", created.getLoader()
        ));
    }
}
