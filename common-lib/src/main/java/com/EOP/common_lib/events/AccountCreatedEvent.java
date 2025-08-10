package com.EOP.common_lib.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
public class AccountCreatedEvent {
    private String email;
    private String username;
    private String password;
    private String verificationToken;
}

