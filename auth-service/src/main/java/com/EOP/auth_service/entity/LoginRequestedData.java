package com.EOP.auth_service.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class LoginRequestedData {
    private String email;
    private String password;

}
