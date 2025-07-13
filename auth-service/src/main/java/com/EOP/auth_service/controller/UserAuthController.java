package com.EOP.auth_service.controller;

import com.EOP.auth_service.DTO.LoginRequestedData;
import com.EOP.auth_service.DTO.UserDTO;
import com.EOP.auth_service.serviceImpl.UserAuthServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserAuthController {
    private final UserAuthServiceImpl userAuthService;

    public UserAuthController(UserAuthServiceImpl userAuthService) {
        this.userAuthService = userAuthService;
    }
    @GetMapping("/test")
    public String test() {
        return "Auth service working!";
    }
    @PostMapping("/login")
    ResponseEntity<String> login(@RequestBody LoginRequestedData credentials){
        return this.userAuthService.login(credentials);
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        return userAuthService.refreshToken(authHeader);
    }
    @GetMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        return userAuthService.logout(authHeader);
    }
    @PostMapping("/addUser")
    ResponseEntity<String> addUser(@RequestBody UserDTO data){
        return this.userAuthService.addUser(data);
    }
}
