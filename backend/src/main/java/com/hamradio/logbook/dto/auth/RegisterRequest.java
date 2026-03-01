package com.hamradio.logbook.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Size(max = 20, message = "Callsign must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9/]+$|^$", message = "Callsign must contain only uppercase letters, numbers, and slashes")
    private String callsign;

    @Size(max = 10, message = "Grid square must not exceed 10 characters")
    private String gridSquare;

    @Size(max = 500, message = "QRZ API key must not exceed 500 characters")
    private String qrzApiKey;
}
