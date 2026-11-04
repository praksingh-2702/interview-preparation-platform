package com.prepmind.prepapi.dto;

import com.prepmind.prepapi.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleRequest(@NotNull(message = "Role must be USER, EDITOR or ADMIN") Role role) {
}
