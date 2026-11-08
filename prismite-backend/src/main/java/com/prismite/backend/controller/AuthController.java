package com.prismite.backend.controller;

import com.prismite.backend.dto.ConnectOAuthRequest;
import com.prismite.backend.dto.LoginRequest;
import com.prismite.backend.dto.RegisterRequest;
import com.prismite.backend.model.User;
import com.prismite.backend.service.AuthService;
import com.prismite.backend.service.DiscordAuthService;
import com.prismite.backend.service.GoogleAuthService;
import com.prismite.backend.service.JwtService;
import com.prismite.backend.service.MicrosoftAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final GoogleAuthService googleAuthService;
    private final DiscordAuthService discordAuthService;
    private final MicrosoftAuthService microsoftAuthService;

    @Value("${google.redirect-uri:http://localhost:8080/api/auth/google/callback}")
    private String googleRedirectUri;

    @Value("${discord.redirect-uri:http://localhost:8080/api/auth/discord/callback}")
    private String discordRedirectUri;

    @Value("${microsoft.redirect-uri:http://localhost:8080/api/auth/microsoft/callback}")
    private String microsoftRedirectUri;

    @Value("${microsoft.tenant-id:consumers}")
    private String microsoftTenantId;

    @Value("${microsoft.client-id:}")
    private String microsoftClientId;

    public AuthController(AuthService authService,
                         JwtService jwtService,
                         GoogleAuthService googleAuthService,
                         DiscordAuthService discordAuthService,
                         MicrosoftAuthService microsoftAuthService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.googleAuthService = googleAuthService;
        this.discordAuthService = discordAuthService;
        this.microsoftAuthService = microsoftAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.register(request.getEmail(), request.getUsername(), request.getPassword()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request.getEmail(), request.getPassword()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authHeader) {
        String email = jwtService.extractEmail(authHeader.replace("Bearer ", ""));
        User user = authService.findByEmail(email);
        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "username", user.getUsername(),
                "microsoftLinked", user.isMicrosoftLinked(),
                "googleLinked", user.isGoogleLinked(),
                "discordLinked", user.isDiscordLinked(),
                "minecraftOwned", user.isMinecraftOwned()
        ));
    }

    @GetMapping("/google/start")
    public ResponseEntity<?> googleStart() {
        return ResponseEntity.ok(Map.of("url", googleAuthService.buildAuthUrl(googleRedirectUri)));
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestParam String code) {
        Map<String, Object> profile = googleAuthService.exchangeCode(code, googleRedirectUri);
        String email = String.valueOf(profile.getOrDefault("email", "user@example.com"));
        String name = String.valueOf(profile.getOrDefault("name", "Google User"));
        String providerUserId = String.valueOf(profile.getOrDefault("externalUserId", "google-user"));

        var authResponse = authService.registerOrLoginExternal("google", email, name);
        authService.connectExternalAccount(email, "google", providerUserId);

        return ResponseEntity.ok(Map.of(
                "token", authResponse.getToken(),
                "username", authResponse.getUsername(),
                "email", email,
                "provider", "google",
                "linked", true
        ));
    }

    @GetMapping("/discord/start")
    public ResponseEntity<?> discordStart() {
        return ResponseEntity.ok(Map.of("url", discordAuthService.buildAuthUrl(discordRedirectUri)));
    }

    @GetMapping("/discord/callback")
    public ResponseEntity<?> discordCallback(@RequestParam String code) {
        Map<String, Object> profile = discordAuthService.exchangeCode(code, discordRedirectUri);
        String email = String.valueOf(profile.getOrDefault("email", "user@example.com"));
        String name = String.valueOf(profile.getOrDefault("name", "Discord User"));
        String providerUserId = String.valueOf(profile.getOrDefault("externalUserId", "discord-user"));
        String accessToken = String.valueOf(profile.getOrDefault("accessToken", ""));

        var authResponse = authService.registerOrLoginExternal("discord", email, name);
        authService.connectExternalAccount(email, "discord", providerUserId);

        boolean joined = discordAuthService.joinGuild(providerUserId, accessToken);

        return ResponseEntity.ok(Map.of(
                "token", authResponse.getToken(),
                "username", authResponse.getUsername(),
                "email", email,
                "provider", "discord",
                "linked", true,
                "guildJoined", joined
        ));
    }

    @GetMapping("/microsoft/start")
    public ResponseEntity<?> microsoftStart() {
        String clientId = System.getenv().getOrDefault("MICROSOFT_CLIENT_ID", "");
        if (clientId.isBlank()) {
            clientId = microsoftClientId;
        }
        return ResponseEntity.ok(Map.of("url", microsoftAuthService.buildAuthUrl(clientId, microsoftRedirectUri, microsoftTenantId)));
    }

    @GetMapping("/microsoft/callback")
    public ResponseEntity<?> microsoftCallback(@RequestParam String code) {
        Map<String, Object> profile = microsoftAuthService.exchangeCode(code, microsoftRedirectUri);
        String email = String.valueOf(profile.getOrDefault("email", "user@example.com"));
        String name = String.valueOf(profile.getOrDefault("name", "Microsoft User"));
        String providerUserId = String.valueOf(profile.getOrDefault("externalUserId", "microsoft-user"));

        var authResponse = authService.registerOrLoginExternal("microsoft", email, name);
        authService.connectExternalAccount(email, "microsoft", providerUserId);

        boolean ownsMinecraft = microsoftAuthService.validateMinecraftOwnership();

        return ResponseEntity.ok(Map.of(
                "token", authResponse.getToken(),
                "username", authResponse.getUsername(),
                "email", email,
                "provider", "microsoft",
                "linked", true,
                "minecraftOwned", ownsMinecraft
        ));
    }

    @PostMapping("/connect-oauth")
    public ResponseEntity<?> connectOAuth(@RequestHeader("Authorization") String authHeader, @RequestBody ConnectOAuthRequest request) {
        String email = jwtService.extractEmail(authHeader.replace("Bearer ", ""));
        User user = authService.connectExternalAccount(email, request.getProvider(), request.getExternalUserId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "provider", request.getProvider(),
                "username", user.getUsername(),
                "linked", true
        ));
    }

    @GetMapping("/discord/guild-join")
    public ResponseEntity<?> discordGuildJoin(@RequestParam String userId, @RequestParam String accessToken) {
        boolean joined = discordAuthService.joinGuild(userId, accessToken);
        return ResponseEntity.ok(Map.of("joined", joined));
    }
}
