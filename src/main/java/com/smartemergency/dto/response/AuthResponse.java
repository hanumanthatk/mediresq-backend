package com.smartemergency.dto.response;

import com.smartemergency.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String fullName;
    private Role role;
    private Long hospitalId;
    private String message;
}
