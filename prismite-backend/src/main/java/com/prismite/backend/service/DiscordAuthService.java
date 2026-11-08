package com.prismite.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class DiscordAuthService {
    @Value("${discord.client-id:}")
    private String clientId;

    @Value("${discord.client-secret:}")
    private String clientSecret;

    @Value("${discord.guild-id:}")
    private String guildId;

    @Value("${discord.bot-token:}")
    private String botToken;

    public String buildAuthUrl(String redirectUri) {
        return "https://discord.com/oauth2/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=identify+email+guilds.join";
    }

    public Map<String, Object> exchangeCode(String code, String redirectUri) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return Map.of("mock", true, "email", "user@example.com", "name", "Discord User", "provider", "discord");
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://discord.com/api/oauth2/token",
                HttpMethod.POST,
                request,
                Map.class
        );

        Map body = response.getBody();
        if (body == null || !body.containsKey("access_token")) {
            return Map.of("mock", true, "email", "user@example.com", "name", "Discord User", "provider", "discord");
        }

        String accessToken = String.valueOf(body.get("access_token"));
        HttpHeaders profileHeaders = new HttpHeaders();
        profileHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeaders);

        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "https://discord.com/api/users/@me",
                HttpMethod.GET,
                profileRequest,
                Map.class
        );

        Map profile = userResponse.getBody();
        if (profile == null) {
            return Map.of("mock", true, "email", "user@example.com", "name", "Discord User", "provider", "discord");
        }

        return Map.of(
                "accessToken", accessToken,
                "email", profile.getOrDefault("email", "user@example.com"),
                "name", profile.getOrDefault("username", "Discord User"),
                "provider", "discord",
                "externalUserId", profile.getOrDefault("id", "discord-user")
        );
    }

    public boolean joinGuild(String userId, String accessToken) {
        if (clientId == null || clientId.isBlank() || guildId == null || guildId.isBlank() || botToken == null || botToken.isBlank()) {
            return true;
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(botToken);

        Map<String, String> body = Map.of("access_token", accessToken);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "https://discord.com/api/guilds/" + guildId + "/members/" + userId,
                HttpMethod.PUT,
                request,
                String.class
        );

        return response.getStatusCode().is2xxSuccessful();
    }
}
