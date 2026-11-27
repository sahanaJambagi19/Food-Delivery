package com.fooddelivery.controller;

import com.fooddelivery.dto.request.*;
import com.fooddelivery.dto.response.*;
import com.fooddelivery.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login, Token Refresh, Logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user (CUSTOMER, RESTAURANT_OWNER, DELIVERY_PARTNER)")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", authService.register(req)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(req)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Get a new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", authService.refreshToken(req)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token (logout)")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }
}
