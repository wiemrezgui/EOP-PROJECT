package com.EOP.auth_service.service;

import com.EOP.auth_service.DTO.LoginRequestedData;

public interface UserAuthService {
    String login(LoginRequestedData data);
}
