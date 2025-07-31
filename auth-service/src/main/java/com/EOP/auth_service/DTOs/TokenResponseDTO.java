package com.EOP.auth_service.DTOs;

import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor @Builder @Setter @Getter
public class TokenResponseDTO {
    private String token;
    private String refreshToken;
    private Date expiresIn;
}