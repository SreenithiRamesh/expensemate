package com.expensemate.dto;

public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private Long userId;
    private String name;
    private String email;

    public LoginResponse() {
    }

    public LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            Long userId,
            String name,
            String email
    ) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}