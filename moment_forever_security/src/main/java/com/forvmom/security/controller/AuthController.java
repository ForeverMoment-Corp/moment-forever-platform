package com.forvmom.security.controller;

import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.security.dto.AuthResponse;
import com.forvmom.security.dto.GoogleSignInRequest;
import com.forvmom.security.service.AuthService;
import com.forvmom.security.dto.LoginRequest;
import com.forvmom.security.dto.RegisterRequestDto;
import com.forvmom.security.service.SocialAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

//TODO: Email Based authentication, Password Reset, Account Verification, etc.
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication API", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;

    @Autowired
    public AuthController(AuthService authService, SocialAuthService socialAuthService) {
        this.authService = authService;
        this.socialAuthService = socialAuthService;
    }


    @PostMapping("/register")
    @Operation(summary = "Register User", description = "Register a new user account")
    public ResponseEntity<ApiResponse<?>> register(
            @Valid @RequestBody RegisterRequestDto request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(null, "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login User", description = "Authenticate user and return JWT token")
    public ResponseEntity<ApiResponse<?>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseUtil.buildOkResponse(response, "User logged in successfully"));
    }

    /**
     * Signs a user in with Google while still issuing the platform's own JWT and
     * refresh-token pair.
     *
     * <p>
     * The client authenticates with Google first and forwards the returned ID token
     * to this endpoint. The backend verifies the token, links or provisions a
     * local account, then creates a normal Moment Forever authenticated session.
     */
    @PostMapping("/social/google")
    @Operation(summary = "Sign in with Google", description = "Verify a Google ID token, link or create the local account, and issue platform JWT tokens")
    public ResponseEntity<ApiResponse<?>> signInWithGoogle(
            @Valid @RequestBody GoogleSignInRequest request) {
        AuthResponse response = socialAuthService.signInWithGoogle(request.getIdToken());
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, "Google sign-in successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Generate a new access token using refresh token")
    public ResponseEntity<AuthResponse> getRefreshToken(
            @RequestHeader("Authorization") String token) {

        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        AuthResponse response = authService.generateRefreshToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout User", description = "Invalidate current session/token")
    public ResponseEntity<ApiResponse<?>> logout(
            @RequestHeader("Authorization") String token) {

        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(token);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseUtil.buildOkResponse(null, "Logout successful"));
    }

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check if auth service is running")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Authentication API is running");
    }
}