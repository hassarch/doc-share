package com.docshare.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed binding for the {@code docshare.jwt.*} properties already present in {@code
 * application.yml} since Phase 2. Kept in {@code config} (not {@code auth}) since it's plain
 * configuration data, not auth business logic — {@code auth.service} classes consume this, they
 * don't own it.
 */
@Component
@ConfigurationProperties(prefix = "docshare.jwt")
public class JwtProperties {

  private String secret;
  private long accessTokenTtlMinutes;
  private long refreshTokenTtlDays;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getAccessTokenTtlMinutes() {
    return accessTokenTtlMinutes;
  }

  public void setAccessTokenTtlMinutes(long accessTokenTtlMinutes) {
    this.accessTokenTtlMinutes = accessTokenTtlMinutes;
  }

  public long getRefreshTokenTtlDays() {
    return refreshTokenTtlDays;
  }

  public void setRefreshTokenTtlDays(long refreshTokenTtlDays) {
    this.refreshTokenTtlDays = refreshTokenTtlDays;
  }
}
