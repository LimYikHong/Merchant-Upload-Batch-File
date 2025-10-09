package rta.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rta.model.MerchantProfile;
import rta.service.ProfileService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final ProfileService profileService;

    public AuthController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * POST /api/auth/login
     * - Accepts a simple body with username/password (using MerchantProfile as a
     * DTO here).
     * - Delegates to ProfileService.login(...) for credential checking.
     * - Returns 200 + user profile on success, or 401 on invalid credentials.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MerchantProfile credentials) {
        MerchantProfile user = profileService.login(credentials.getUsername(), credentials.getPassword());
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }
}
