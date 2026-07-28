package com.docshare.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data JPA auditing, which is what actually populates {@code
 * BaseEntity.createdAt}/{@code updatedAt} on insert/update. Without this annotation present
 * somewhere, {@code @CreatedDate}/{@code @LastModifiedDate} are silently ignored and those columns
 * stay null — a common, quiet bug if this class is ever deleted "because it looks empty."
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
