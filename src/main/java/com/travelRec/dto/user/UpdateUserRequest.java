package com.travelRec.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(min = 1, max = 50)
    private String firstName;

    @Size(min = 1, max = 50)
    private String lastName;

    @Size(max = 500)
    private String avatarUrl;
}
