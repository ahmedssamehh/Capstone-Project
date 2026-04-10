package com.workhub.dto;

import com.workhub.entity.User;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    
    @Email(message = "Email must be valid")
    private String email;
    
    private User.UserRole role;
    private User.UserStatus status;
}
