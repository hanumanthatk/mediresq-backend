package com.smartemergency.dto.response;

import com.smartemergency.enums.Role;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Role role;
    private Boolean isActive;
    private Boolean isVerified;
    private String address;
    private String bloodGroup;
    private String emergencyContact;
    private String profileImage;
    private LocalDateTime createdAt;
}
