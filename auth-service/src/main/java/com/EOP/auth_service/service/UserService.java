package com.EOP.auth_service.service;

import com.EOP.auth_service.DTOs.UserDTO;
import com.EOP.auth_service.DTOs.UserResponseDTO;
import com.EOP.auth_service.exception.InvalidDataException;
import com.EOP.auth_service.exception.UserExistsException;
import com.EOP.auth_service.jwt.JwtTokenProvider;
import com.EOP.auth_service.models.User;
import com.EOP.auth_service.repository.UserAuthRepository;
import com.EOP.common_lib.common.enums.Department;
import com.EOP.common_lib.common.enums.Role;
import com.EOP.common_lib.common.exceptions.ResourceNotFoundException;
import com.EOP.common_lib.events.AccountCreatedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
@Slf4j
@Service
@AllArgsConstructor
public class UserService {
    private final UserAuthRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGeneratorService passwordGeneratorService;
    private final KafkaTemplate<String, AccountCreatedEvent> kafkaTemplate;
    private final JwtTokenProvider jwtTokenProvider;

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

    public boolean checkUserExists(String userEmail) {
        Optional<User> existingUser = userRepository.findByEmail(userEmail.trim().toLowerCase());
        return existingUser.isPresent();
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
}
