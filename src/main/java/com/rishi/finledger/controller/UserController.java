package com.rishi.finledger.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rishi.finledger.dto.ApiResponseDTO;
import com.rishi.finledger.dto.AuthResponse;
import com.rishi.finledger.dto.LoginRequest;
import com.rishi.finledger.dto.LogoutRequest;
import com.rishi.finledger.dto.RefreshTokenRequest;
import com.rishi.finledger.dto.RegisterRequest;
import com.rishi.finledger.dto.UserResponse;
import com.rishi.finledger.service.auth.UserService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication", description = "User registration, login, logout and token management APIs")
@RestController
@RequestMapping("/auth")
public class UserController {
    private final UserService userService;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService usersService) {
        this.userService = usersService;
    }

    @Operation(summary = "Health check", description = "Verifies that the backend is running")
    @GetMapping("/")
    public String home() {
        return "Backend is LIVE 🚀";
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account and wallet")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })

    @PostMapping("/register")
    public ApiResponseDTO<UserResponse> register(@Valid @RequestBody RegisterRequest request) {

        log.info("API HIT | POST /auth/register | email={}", request.getEmail());

        UserResponse user = userService.registerUser(request);

        log.info("User registered | userId={}", user.getId());

        return new ApiResponseDTO<UserResponse>("User registered successfully", user);
    }

    @Operation(summary = "Login user", description = "Authenticates user and returns access & refresh tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Validation failed")
    })

    @PostMapping("/login")
    public ApiResponseDTO<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        log.info("API HIT | POST /auth/login | email={}", request.getEmail());

        AuthResponse token = userService.loginUser(request);

        log.info("Login success | email={}", request.getEmail());

        return new ApiResponseDTO<AuthResponse>("Login successful", token);
    }

    @Operation(summary = "Refresh access token", description = "Generates a new access token using a valid refresh token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshTokenRequest request) {
        return userService.refreshAccessToken(request.getRefreshToken());
    }

    @Operation(summary = "Logout user", description = "Revokes the provided refresh token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<String>> logout(
            @RequestBody LogoutRequest request) {

        log.info("API HIT | POST /auth/logout");

        userService.logout(request.getRefreshToken());

        log.info("Logout success");

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Logged out successfully", null));
    }
}
