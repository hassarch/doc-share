package com.docshare.backend.auth.dto;

public record TokenPairResponse(String accessToken, String refreshToken, long expiresInSeconds) {}
