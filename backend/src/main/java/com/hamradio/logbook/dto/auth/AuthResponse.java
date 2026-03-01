package com.hamradio.logbook.dto.auth;

import com.hamradio.logbook.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private Long userId;
    private String username;
    private String callsign;
    private String fullName;
    private Set<User.Role> roles;

    public AuthResponse(String token, Long userId, String username,
                        String callsign, String fullName, Set<User.Role> roles) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.callsign = callsign;
        this.fullName = fullName;
        this.roles = roles;
    }
}
