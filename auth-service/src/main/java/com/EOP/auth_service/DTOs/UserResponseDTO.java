package com.EOP.auth_service.DTOs;

import com.EOP.auth_service.model.Role;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder @Slf4j @Setter
public class UserResponseDTO {
    private String uuid;
    private String email;
    private String username;
    private Role role;
    private String department;
    private boolean verified;
}
