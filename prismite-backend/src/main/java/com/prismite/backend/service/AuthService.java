package com.prismite.backend.service;

import com.prismite.backend.model.User;
import com.prismite.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(String email, String username, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = new User(email, username, passwordEncoder.encode(password));
        user.setEmailVerified(true);
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getUsername(), user.getEmail());
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getUsername(), user.getEmail());
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public AuthResponse registerOrLoginExternal(String provider, String email, String username) {
        Optional<User> existing = userRepository.findByEmail(email);
        User user = existing.orElseGet(() -> {
            String generatedUsername = username == null || username.isBlank() ? email.split("@")[0] : username;
            String uniqueUsername = generatedUsername;
            int candidate = 1;
            while (userRepository.existsByUsername(uniqueUsername)) {
                uniqueUsername = generatedUsername + candidate;
                candidate++;
            }
            return userRepository.save(new User(email, uniqueUsername, passwordEncoder.encode("oauth-user-" + System.nanoTime())));
        });

        user.setEmailVerified(true);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getUsername(), user.getEmail());
    }

    public User connectExternalAccount(String email, String provider, String externalUserId) {
        User user = findByEmail(email);

        switch (provider.toLowerCase()) {
            case "google" -> {
                user.setGoogleLinked(true);
                user.setGoogleUserId(externalUserId);
            }
            case "discord" -> {
                user.setDiscordLinked(true);
                user.setDiscordUserId(externalUserId);
            }
            case "microsoft" -> {
                user.setMicrosoftLinked(true);
                user.setMicrosoftUserId(externalUserId);
                user.setMinecraftOwned(true);
            }
            default -> throw new IllegalArgumentException("Unsupported provider");
        }

        return userRepository.save(user);
    }
}
