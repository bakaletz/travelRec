package com.travelRec.dto.auth;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
