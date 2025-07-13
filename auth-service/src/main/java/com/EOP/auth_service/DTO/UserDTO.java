package com.EOP.auth_service.DTO;

import com.EOP.auth_service.model.Department;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class UserDTO {
    String username;
    String email;
    String password;
    Department department;
}
