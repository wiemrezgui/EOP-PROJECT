package com.EOP.auth_service.controller;

import com.EOP.auth_service.DTO.LoginRequestedData;
import com.EOP.auth_service.serviceImpl.UserAuthServiceImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserAuthController {
    private final UserAuthServiceImpl userAuthService;

    public UserAuthController(UserAuthServiceImpl userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/login")
    String login(@RequestBody LoginRequestedData credentials){
        return this.userAuthService.login(credentials);
    }
}
