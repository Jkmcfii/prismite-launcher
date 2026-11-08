package com.prismite.backend.service;

import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {
    public String buildAuthUrl(String redirectUri) {
        return "https://accounts.google.com/o/oauth2/v2/auth?client_id=GOOGLE_CLIENT_ID&redirect_uri="
                + redirectUri
                + "&response_type=code&scope=openid%20email%20profile";
    }
}
