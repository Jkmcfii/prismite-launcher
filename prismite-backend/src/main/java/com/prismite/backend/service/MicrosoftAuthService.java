package com.prismite.backend.service;

import org.springframework.stereotype.Service;

@Service
public class MicrosoftAuthService {
    public String buildAuthUrl(String clientId, String redirectUri, String tenantId) {
        return "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/authorize"
                + "?client_id=" + clientId
                + "&response_type=code"
                + "&redirect_uri=" + redirectUri
                + "&response_mode=query"
                + "&scope=openid%20profile%20offline_access";
    }

    public boolean validateMinecraftOwnership() {
        return true;
    }
}
