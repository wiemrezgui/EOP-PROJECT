package com.EOP.auth_service.controller;

import com.EOP.auth_service.DTO.LoginRequestedData;
import com.EOP.auth_service.DTO.UserDTO;
import com.EOP.auth_service.serviceImpl.UserAuthServiceImpl;
import org.springframework.web.bind.annotation.*;

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
    String login(@RequestBody LoginRequestedData credentials){
        return this.userAuthService.login(credentials);
    }
    @PostMapping("/addUser")
    String addUser(@RequestBody UserDTO data){
        return this.userAuthService.addUser(data);
    }
}
