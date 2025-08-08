package com.EOP.auth_service.service;

import com.EOP.auth_service.DTOs.*;
import com.EOP.auth_service.exception.*;
import com.EOP.auth_service.jwt.JwtTokenProvider;
import com.EOP.auth_service.models.User;
import com.EOP.auth_service.repository.UserAuthRepository;
import com.EOP.common_lib.common.enums.Department;
import com.EOP.common_lib.common.enums.Role;
import com.EOP.common_lib.common.exceptions.ResourceNotFoundException;
import events.AccountCreatedEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class UserAuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserAuthRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordGeneratorService passwordGeneratorService;
    private final KafkaTemplate<String, AccountCreatedEvent> kafkaTemplate;

    public LoginResponseDTO login(LoginRequestedDataDTO data) {
        try {
            log.info("Attempting login for email: {}", data.getEmail());

            // Validate input
            if (data.getEmail() == null || data.getEmail().trim().isEmpty()) {
                throw new InvalidDataException("Email is required");
            }

            if (data.getPassword() == null || data.getPassword().trim().isEmpty()) {
                throw new InvalidDataException("Password is required");
            }

            // Check if user exists first
            try {
                userDetailsService.loadUserByUsername(data.getEmail());
            } catch (ResourceNotFoundException e) {
                log.warn("Login attempt for non-existent user: {}", data.getEmail());
                throw new ResourceNotFoundException("No account found with this email");
            }

            // Authenticate user
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                data.getEmail().trim().toLowerCase(),
                                data.getPassword()
                        )
                );

                if (!authentication.isAuthenticated()) {
                    throw new AuthenticationFailedException("Authentication failed");
                }

                // Get user details
                User user = userRepository.findByEmail(data.getEmail())
                        .orElseThrow(() -> new ResourceNotFoundException("No user found with this email"));
                        ;
                // Generate tokens
                String token = jwtTokenProvider.generateTokenFromUser(user);
                String refreshToken = jwtTokenProvider.generateRefreshToken(data.getEmail());

                log.info("Login successful for email: {}", data.getEmail());

                return LoginResponseDTO.builder()
                        .accessToken(token)
                        .refreshToken(refreshToken)
                        .build();

            } catch (BadCredentialsException e) {
                log.warn("Invalid password for user: {}", data.getEmail());
                throw new AuthenticationFailedException("Please check your password");
            } catch (DisabledException e) {
                log.warn("Login attempt for disabled user: {}", data.getEmail());
                throw new AuthenticationFailedException("Account is disabled");
            } catch (LockedException e) {
                log.warn("Login attempt for locked user: {}", data.getEmail());
                throw new AuthenticationFailedException("Account is locked");
            } catch (AuthenticationException e) {
                log.error("Authentication error for user {}: {}", data.getEmail(), e.getMessage());
                throw new AuthenticationFailedException("Authentication failed");
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        }
    }

    public UserResponseDTO addUser(UserDTO data) {
        try {
            log.info("Creating new user account for email: {}", data.getEmail());

            // Validate input data
            validateUserData(data);

            // Check if user already exists
            Optional<User> existingUser = userRepository.findByEmail(data.getEmail().trim().toLowerCase());
            if (existingUser.isPresent()) {
                throw new UserExistsException("An account with this email already exists");
            }

            // Create new user
            User newUser = new User();
            String generatedPassword = passwordGeneratorService.generatePassword();

            newUser.setEmail(data.getEmail().trim().toLowerCase());
            newUser.setPassword(passwordEncoder.encode(generatedPassword));
            newUser.setDepartment(Department.valueOf(data.getDepartment()));
            newUser.setRole(Role.EMPLOYER);
            newUser.setUsername(data.getUsername().trim());
            newUser.setVerified(false);

            User savedUser = userRepository.save(newUser);

            // Generate verification token
            String verificationToken = jwtTokenProvider.generateToken(
                    new org.springframework.security.core.userdetails.User(
                            savedUser.getEmail(),
                            savedUser.getPassword(),
                            Collections.emptyList()
                    )
            );

            // Send account creation event
            try {
                kafkaTemplate.send(
                        "account-created",
                        new AccountCreatedEvent(
                                savedUser.getEmail(),
                                savedUser.getUsername(),
                                generatedPassword,
                                verificationToken
                        )
                );
                log.info("Account creation event sent for user: {}", savedUser.getEmail());
            } catch (Exception e) {
                log.error("Failed to send account creation event for user {}: {}", savedUser.getEmail(), e.getMessage());
                // Don't fail the user creation process if event sending fails
            }

            log.info("Successfully created user account: {}", savedUser.getEmail());

            return UserResponseDTO.builder()
                    .uuid(savedUser.getUuid())
                    .email(savedUser.getEmail())
                    .username(savedUser.getUsername())
                    .department(String.valueOf(savedUser.getDepartment()))
                    .verified(savedUser.isVerified())
                    .build();

        } catch (Exception e) {
            if (e instanceof UserExistsException || e instanceof InvalidDataException) {
                throw e;
            }
            log.error("Error creating user account for email {}: {}", data.getEmail(), e.getMessage(), e);
            throw new InvalidDataException("Failed to create user account. Please try again.");
        }
    }


    public TokenResponseDTO refreshToken(HttpServletRequest  request) {
        try {
            String refreshToken = extractTokenFromRequest(request);

            if (!jwtTokenProvider.validateToken(refreshToken)) {
                throw new InvalidTokenException("Invalid or expired refresh token");
            }

            String email = jwtTokenProvider.extractUsername(refreshToken);
            if (email == null || email.trim().isEmpty()) {
                throw new InvalidTokenException("Invalid token format");
            }

            // Verify user still exists
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Generate new tokens
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    Collections.emptyList()
            );

            String newToken = jwtTokenProvider.generateToken(userDetails);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);
            Date expiresIn = jwtTokenProvider.extractExpiration(newToken);

            log.info("Token refreshed successfully for user: {}", email);

            return TokenResponseDTO.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(expiresIn)
                    .build();

        } catch (Exception e) {
            if (e instanceof InvalidTokenException || e instanceof ResourceNotFoundException) {
                throw e;
            }
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw new InvalidTokenException("Failed to refresh token");
        }
    }

    public void logout(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);

            if (!jwtTokenProvider.validateToken(token)) {
                throw new InvalidTokenException("Invalid token");
            }

            tokenBlacklistService.blacklistToken(token);

            String email = jwtTokenProvider.extractUsername(token);
            log.info("User logged out successfully: {}", email);

        } catch (Exception e) {
            if (e instanceof InvalidTokenException) {
                throw e;
            }
            log.error("Error during logout: {}", e.getMessage(), e);
            throw new InvalidTokenException("Logout failed");
        }
    }

    public void verifyAccount(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                throw new InvalidDataException("Email is required");
            }

            if (!isValidEmail(email)) {
                throw new InvalidDataException("Please provide a valid email address");
            }

            User user = userRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

            if (user.isVerified()) {
                throw new InvalidDataException("Account is already verified");
            }

            user.setVerified(true);
            userRepository.save(user);

            log.info("Account verified successfully for user: {}", email);

        } catch (Exception e) {
            if (e instanceof ResourceNotFoundException || e instanceof InvalidDataException) {
                throw e;
            }
            log.error("Error verifying account for email {}: {}", email, e.getMessage(), e);
            throw new InvalidDataException("Failed to verify account. Please try again.");
        }
    }
    private void validateUserData(UserDTO data) {
        if (data.getEmail() == null || data.getEmail().trim().isEmpty()) {
            throw new InvalidDataException("Email is required");
        }

        if (!isValidEmail(data.getEmail())) {
            throw new InvalidDataException("Please provide a valid email address");
        }

        if (data.getUsername() == null || data.getUsername().trim().isEmpty()) {
            throw new InvalidDataException("Username is required");
        }

        if (data.getUsername().length() < 3 || data.getUsername().length() > 50) {
            throw new InvalidDataException("Username must be between 3 and 50 characters");
        }

        if (data.getDepartment() == null || data.getDepartment().trim().isEmpty()) {
            throw new InvalidDataException("Department is required");
        }

        if (data.getDepartment().length() < 2 || data.getDepartment().length() > 100) {
            throw new InvalidDataException("Department must be between 2 and 100 characters");
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String token = null;

        // Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            if (authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            } else {
                token = authHeader;
            }
        }

        // If no token from header, try request parameter
        if (token == null || token.trim().isEmpty()) {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.trim().isEmpty()) {
                token = tokenParam;
            }
        }

        // If still no token found, throw exception
        if (token == null || token.trim().isEmpty()) {
            throw new InvalidTokenException("No authentication token found in request");
        }

        return token;
    }
    public boolean checkUserExists(String userUUID) {
        return userRepository.existsById(userUUID);
    }
}

