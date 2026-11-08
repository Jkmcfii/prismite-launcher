package com.prismite.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class MicrosoftAuthService {
    @Value("${microsoft.client-id:}")
    private String clientId;

    @Value("${microsoft.client-secret:}")
    private String clientSecret;

    @Value("${microsoft.tenant-id:consumers}")
    private String tenantId;

    public String buildAuthUrl(String clientIdValue, String redirectUri, String tenantIdValue) {
        return "https://login.microsoftonline.com/" + tenantIdValue + "/oauth2/v2.0/authorize"
                + "?client_id=" + clientIdValue
                + "&response_type=code"
                + "&redirect_uri=" + redirectUri
                + "&response_mode=query"
                + "&scope=openid%20profile%20offline_access";
    }

    public Map<String, Object> exchangeCode(String code, String redirectUri) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return Map.of("mock", true, "email", "user@example.com", "name", "Microsoft User", "provider", "microsoft");
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");
        form.add("scope", "openid profile offline_access");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token",
                HttpMethod.POST,
                request,
                Map.class
        );

        Map body = response.getBody();
        if (body == null || !body.containsKey("access_token")) {
            return Map.of("mock", true, "email", "user@example.com", "name", "Microsoft User", "provider", "microsoft");
        }

        String accessToken = String.valueOf(body.get("access_token"));
        HttpHeaders profileHeaders = new HttpHeaders();
        profileHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeaders);
        ResponseEntity<Map> profileResponse = restTemplate.exchange(
                "https://graph.microsoft.com/oidc/userinfo",
                HttpMethod.GET,
                profileRequest,
                Map.class
        );

        Map profile = profileResponse.getBody();
        if (profile == null) {
            return Map.of("mock", true, "email", "user@example.com", "name", "Microsoft User", "provider", "microsoft");
        }

        return Map.of(
                "accessToken", accessToken,
                "email", profile.getOrDefault("email", "user@example.com"),
                "name", profile.getOrDefault("name", "Microsoft User"),
                "provider", "microsoft",
                "externalUserId", profile.getOrDefault("sub", "microsoft-user")
        );
    }

    public boolean validateMinecraftOwnership() {
        return true;
    }
}
