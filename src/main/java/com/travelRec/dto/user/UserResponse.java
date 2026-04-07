package com.travelRec.dto.user;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String role;
    private LocalDateTime createdAt;
}
