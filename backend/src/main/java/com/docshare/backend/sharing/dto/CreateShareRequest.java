package com.docshare.backend.sharing.dto;

import com.docshare.backend.sharing.entity.PermissionRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateShareRequest(
    @NotNull UUID documentId, @NotBlank @Email String email, @NotNull PermissionRole role) {}
