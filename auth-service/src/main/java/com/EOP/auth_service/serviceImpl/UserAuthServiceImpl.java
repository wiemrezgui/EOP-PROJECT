package com.EOP.auth_service.serviceImpl;

import com.EOP.auth_service.DTO.LoginRequestedData;
import com.EOP.auth_service.DTO.UserDTO;
import com.EOP.auth_service.exceptions.InvalidDataException;
import com.EOP.auth_service.exceptions.UserExistsException;
import com.EOP.auth_service.exceptions.UserNotFoundException;
import com.EOP.auth_service.jwt.JwtTokenProvider;
import com.EOP.auth_service.model.Role;
import com.EOP.auth_service.model.User;
import com.EOP.auth_service.repository.UserAuthRepository;
import com.EOP.auth_service.service.UserAuthService;
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
import java.util.Optional;

@Service
public class UserAuthServiceImpl implements UserAuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserAuthRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserAuthServiceImpl(AuthenticationManager authenticationManager,
                               JwtTokenProvider jwtTokenProvider,
                               UserDetailsService userDetailsService,
                               UserAuthRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String login(LoginRequestedData data) {
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
                return jwtTokenProvider.generateToken(userDetails);
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
    public String addUser(UserDTO data) {
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
        return jwtTokenProvider.generateToken(
                new org.springframework.security.core.userdetails.User(
                        addedUser.getEmail(),
                        addedUser.getPassword(),
                        Collections.emptyList()
                )
        );
    }
}

