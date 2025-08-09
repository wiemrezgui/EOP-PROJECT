package com.EOP.auth_service.controller;

import com.EOP.auth_service.DTOs.*;
import com.EOP.auth_service.service.UserAuthService;
import com.EOP.common_lib.common.DTO.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
@Slf4j
@Tag(name = "User auth", description = "User auth apis")
public class UserAuthController {

    private final UserAuthService userAuthService;

    public UserAuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }
    @Operation(summary = "Test service")
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        ApiResponse<String> response = ApiResponse.success(
                "Auth service working!",
                "Service is healthy"
        );
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Login")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @RequestBody @Valid LoginRequestedDataDTO credentials) {

        log.info("Login attempt for email: {}", credentials.getEmail());

        LoginResponseDTO loginResponse = userAuthService.login(credentials);
        ApiResponse<LoginResponseDTO> response = ApiResponse.success(
                loginResponse,
                "Login successful"
        );

        return ResponseEntity.ok(response);
    }
    @Operation(summary = "refresh token")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponseDTO>> refreshToken(
            HttpServletRequest request) {

        TokenResponseDTO tokenResponse = userAuthService.refreshToken(request);
        ApiResponse<TokenResponseDTO> response = ApiResponse.success(
                tokenResponse,
                "Token refreshed successfully"
        );

        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Log out")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            HttpServletRequest request) {


        userAuthService.logout(request);
        ApiResponse<String> response = ApiResponse.success(
                null,
                "Logged out successfully"
        );

        return ResponseEntity.ok(response);
    }

}
