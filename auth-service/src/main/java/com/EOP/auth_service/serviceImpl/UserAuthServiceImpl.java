package com.EOP.auth_service.serviceImpl;

import com.EOP.auth_service.DTO.LoginRequestedData;
import com.EOP.auth_service.config.JwtTokenProvider;
import com.EOP.auth_service.service.UserAuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAuthServiceImpl implements UserAuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    public UserAuthServiceImpl(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }
    @Override
    public String login(LoginRequestedData data) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        data.getEmail(),
                        data.getPassword()
                )
        );

        // If authentication succeeds, generate token
        if (authentication.isAuthenticated()) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(data.getEmail());
            return jwtTokenProvider.generateToken(userDetails);
        } else {
            throw new UsernameNotFoundException("Invalid username or password");
        }
    }
}
