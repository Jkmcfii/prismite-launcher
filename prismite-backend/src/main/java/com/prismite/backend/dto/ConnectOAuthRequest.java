package com.prismite.backend.dto;

public class ConnectOAuthRequest {
    private String provider;
    private String externalUserId;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getExternalUserId() { return externalUserId; }
    public void setExternalUserId(String externalUserId) { this.externalUserId = externalUserId; }
}
