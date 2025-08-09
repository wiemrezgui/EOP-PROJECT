package com.EOP.auth_service.controller;

import com.EOP.auth_service.DTOs.UserDTO;
import com.EOP.auth_service.DTOs.UserResponseDTO;
import com.EOP.auth_service.service.UserAuthService;
import com.EOP.common_lib.common.DTO.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Validated
@Slf4j
@Tag(name = "User", description = "Users apis")
@AllArgsConstructor
public class UserController {
    private final UserAuthService userAuthService;

    @Operation(summary = "Add user by admin")
    @PostMapping("/add-user")
    public ResponseEntity<ApiResponse<UserResponseDTO>> addUser(
            @RequestBody @Valid UserDTO data) {

        log.info("Creating new user account for email: {}", data.getEmail());

        UserResponseDTO userResponse = userAuthService.addUser(data);
        ApiResponse<UserResponseDTO> response = ApiResponse.success(
                userResponse,
                "Account created successfully. Please check your email to verify your account."
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @Operation(summary = "Verify account")
    @GetMapping("/verify-account/{email}")
    public ResponseEntity<ApiResponse<String>> verifyAccount(
            @PathVariable @Email(message = "Please provide a valid email address") String email) {

        log.info("Verifying account for email: {}", email);

        userAuthService.verifyAccount(email);
        ApiResponse<String> response = ApiResponse.success(
                null,
                "Account verified successfully"
        );

        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Check user existance")
    @GetMapping("/check-user/{userEmail}")
    public Boolean checkUser(
            @PathVariable String userEmail) {
        return this.userAuthService.checkUserExists(userEmail);
    }
}
