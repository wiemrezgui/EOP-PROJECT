package com.EOP.auth_service.service;

import com.EOP.auth_service.exceptions.UserNotFoundException;
import com.EOP.auth_service.repository.UserAuthRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAuthRepository userRepository;

    public CustomUserDetailsService(UserAuthRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(email)
                .map(user -> new User(user.getEmail(), user.getPassword(), Collections.emptyList()))
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

}