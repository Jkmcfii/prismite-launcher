package com.prismite.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
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

    public boolean joinGuild(String userId, String accessToken) {
        if (clientId == null || clientId.isBlank() || guildId == null || guildId.isBlank() || botToken == null || botToken.isBlank()) {
            return true;
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(botToken);

        Map<String, String> body = new HashMap<>();
        body.put("access_token", accessToken);

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
