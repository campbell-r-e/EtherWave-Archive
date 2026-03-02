package com.hamradio.logbook.controller;

import com.hamradio.logbook.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller for user preference endpoints
 * Manages per-user settings that persist across sessions
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // TODO: Configure properly in production
public class UserPreferencesController {

    private final UserService userService;

    /**
     * GET /api/user/station-colors
     * Load the authenticated user's station color preferences.
     * Returns 204 No Content when the user has never saved custom colors
     * (frontend should fall back to defaults).
     */
    @GetMapping("/station-colors")
    public ResponseEntity<String> getStationColors(Authentication auth) {
        String username = auth.getName();
        return userService.getStationColorPreferences(username)
                .filter(json -> json != null && !json.isBlank())
                .map(json -> ResponseEntity.ok()
                        .header("Content-Type", "application/json")
                        .body(json))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * PUT /api/user/station-colors
     * Save the authenticated user's station color preferences.
     * Expects a raw JSON body: { "station1": "#hex", ..., "gota": "#hex" }
     */
    @PutMapping(value = "/station-colors", consumes = "application/json")
    public ResponseEntity<Map<String, String>> saveStationColors(
            Authentication auth,
            @RequestBody String colorsJson) {
        String username = auth.getName();
        userService.saveStationColorPreferences(username, colorsJson);
        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    /**
     * DELETE /api/user/station-colors
     * Reset the authenticated user's station colors to application defaults.
     */
    @DeleteMapping("/station-colors")
    public ResponseEntity<Map<String, String>> resetStationColors(Authentication auth) {
        String username = auth.getName();
        userService.resetStationColorPreferences(username);
        return ResponseEntity.ok(Map.of("status", "reset"));
    }
}
