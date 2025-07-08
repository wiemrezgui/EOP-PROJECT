package com.EOP.auth_service.controller;

import com.EOP.auth_service.DTO.LoginRequestedData;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
@RestController
@RequestMapping("/api/auth")
public class UserAuthController {
    @PostMapping("/login")
    Map<String,String> login(@RequestBody LoginRequestedData credentials){
        return null;
    }
}
