package com.EOP.auth_service.service;

import com.EOP.auth_service.DTO.LoginRequestedData;
import com.EOP.auth_service.DTO.UserDTO;

public interface UserAuthService {
    String login(LoginRequestedData data);
    String addUser(UserDTO data);
}
