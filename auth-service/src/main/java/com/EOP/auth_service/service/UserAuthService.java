package com.EOP.auth_service.service;

import com.EOP.auth_service.DTO.LoginRequestedData;
import com.EOP.auth_service.DTO.UserDTO;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface UserAuthService {
    ResponseEntity<String> login(LoginRequestedData data);
    ResponseEntity<String> addUser(UserDTO data);
    ResponseEntity<Map<String, String>> refreshToken(String authHeader);
    ResponseEntity<Map<String, String>> logout(String token);
}
