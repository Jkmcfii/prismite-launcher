package com.prismite.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleAuthService {
    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.client-secret:}")
    private String clientSecret;

    public String buildAuthUrl(String redirectUri) {
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=openid%20email%20profile"
                + "&access_type=offline"
                + "&prompt=consent";
    }

    public Map<String, Object> exchangeCode(String code, String redirectUri) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return Map.of("mock", true, "email", "user@example.com", "name", "Google User", "provider", "google");
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                request,
                Map.class
        );

        Map body = response.getBody();
        if (body == null || !body.containsKey("access_token")) {
            return Map.of("mock", true, "email", "user@example.com", "name", "Google User", "provider", "google");
        }

        String accessToken = String.valueOf(body.get("access_token"));
        HttpHeaders profileHeaders = new HttpHeaders();
        profileHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeaders);
        ResponseEntity<Map> profileResponse = restTemplate.exchange(
                "https://openidconnect.googleapis.com/v1/userinfo",
                HttpMethod.GET,
                profileRequest,
                Map.class
        );

        Map profile = profileResponse.getBody();
        if (profile == null) {
            return Map.of("mock", true, "email", "user@example.com", "name", "Google User", "provider", "google");
        }

        return Map.of(
                "accessToken", accessToken,
                "email", profile.getOrDefault("email", "user@example.com"),
                "name", profile.getOrDefault("name", "Google User"),
                "provider", "google",
                "externalUserId", profile.getOrDefault("sub", "google-user")
        );
    }
}
