package com.EOP.auth_service.serviceImpl;

import com.EOP.auth_service.DTO.LoginRequestedData;
import com.EOP.auth_service.DTO.UserDTO;
import com.EOP.auth_service.exceptions.InvalidDataException;
import com.EOP.auth_service.exceptions.InvalidTokenException;
import com.EOP.auth_service.exceptions.UserExistsException;
import com.EOP.auth_service.exceptions.UserNotFoundException;
import com.EOP.auth_service.jwt.JwtTokenProvider;
import com.EOP.auth_service.model.Role;
import com.EOP.auth_service.model.User;
import com.EOP.auth_service.repository.UserAuthRepository;
import com.EOP.auth_service.service.UserAuthService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserAuthRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public ResponseEntity<String> login(LoginRequestedData data) {
        try {
            userDetailsService.loadUserByUsername(data.getEmail());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            data.getEmail(),
                            data.getPassword()
                    )
            );

            if (authentication.isAuthenticated()) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String token = jwtTokenProvider.generateToken(userDetails);
                return ResponseEntity.ok(token);
            } else {
                throw new InvalidDataException("Invalid credentials");
            }

        } catch (UserNotFoundException e) {
            throw e;
        } catch (BadCredentialsException e) {
            throw new InvalidDataException("Invalid credentials");
        } catch (AuthenticationException e) {
            throw new InvalidDataException("Authentication failed");
        }
    }

    @Override
    public ResponseEntity<String> addUser(UserDTO data) {
        Optional<User> existantUser = userRepository.findByEmail(data.getEmail());
        if(data.getEmail() == null || data.getEmail().isBlank()){
            throw new InvalidDataException("Email is required");
        }
        if (existantUser.isPresent()) {
            throw new UserExistsException("User already exists");
        }
        if (data.getPassword() == null || data.getPassword().isBlank() ||
                data.getUsername() == null || data.getUsername().isBlank() ||
                data.getDepartment() == null ) {
            throw new InvalidDataException("Missing or invalid data");
        }

        User newUser = new User();
        newUser.setEmail(data.getEmail());
        newUser.setPassword(this.passwordEncoder.encode(data.getPassword()));
        newUser.setDepartment(data.getDepartment());
        newUser.setRole(Role.EMPLOYER);
        newUser.setUsername(data.getEmail());
        User addedUser = this.userRepository.save(newUser);

        String token = jwtTokenProvider.generateToken(
                new org.springframework.security.core.userdetails.User(
                        addedUser.getEmail(),
                        addedUser.getPassword(),
                        Collections.emptyList()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @Override
    public ResponseEntity<Map<String, String>> refreshToken(String authHeader) {
        String refreshToken = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        String email = jwtTokenProvider.extractUsername(refreshToken);
        String newToken = jwtTokenProvider.generateRefreshToken(email);
        Map<String, String> response = new HashMap<>();
        response.put("token", newToken);
        response.put("refreshToken", refreshToken);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Map<String, String>> logout(String token) {
        String authToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        tokenBlacklistService.blacklistToken(authToken);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");

        return ResponseEntity.ok(response);
    }
}

