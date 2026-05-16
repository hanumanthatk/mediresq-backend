package com.smartemergency.service;

import com.smartemergency.dto.request.LoginRequest;
import com.smartemergency.dto.request.RegisterRequest;
import com.smartemergency.dto.response.AuthResponse;
import com.smartemergency.entity.User;
import com.smartemergency.exception.BusinessException;
import com.smartemergency.repository.UserRepository;
import com.smartemergency.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service - handles user registration and login.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user in the system.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        // Check for duplicate phone
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Phone number already registered: " + request.getPhone());
        }

       // Build user entity
User user = User.builder()
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .email(request.getEmail())
        .phone(request.getPhone())
        .password(passwordEncoder.encode(request.getPassword()))
       .role(request.getRole())
        .address(request.getAddress())
        .bloodGroup(request.getBloodGroup())
        .emergencyContact(request.getEmergencyContact())
        .isActive(true)
        .isVerified(false)
        .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} with role: {}", savedUser.getEmail(), savedUser.getRole());

        // Generate tokens
        String accessToken = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .message("Registration successful! Welcome to Smart Emergency System.")
                .build();
    }

    /**
     * Authenticate existing user and return JWT tokens.
     */
    public AuthResponse login(LoginRequest request) {
        // Spring Security handles credential verification
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException("Account is deactivated. Please contact support.");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());

        // Get hospitalId if user is hospital role
        Long hospitalId = null;
        // (resolved in controller via hospital service)

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .message("Login successful!")
                .build();
    }

    /**
     * Refresh access token using refresh token.
     */
    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new BusinessException("Refresh token is invalid or expired");
        }

        String newAccessToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
