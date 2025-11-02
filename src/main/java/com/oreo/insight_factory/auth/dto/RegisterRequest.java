package com.oreo.insight_factory.auth.dto;

import com.oreo.insight_factory.users.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "El username es obligatorio")
    @Pattern(
            regexp = "^[a-zA-Z0-9_.]{3,30}$",
            message = "El username debe tener entre 3 y 30 caracteres, y solo puede contener letras, números, '_' o '.'"
    )
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Role role;

    // Branch obligatorio solo si el rol es BRANCH (validado en AuthService)
    private String branch;
}
