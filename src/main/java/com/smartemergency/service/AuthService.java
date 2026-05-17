package com.smartemergency.service;

import com.smartemergency.dto.request.LoginRequest;
import com.smartemergency.dto.request.RegisterRequest;
import com.smartemergency.dto.response.AuthResponse;
import com.smartemergency.entity.Hospital;
import com.smartemergency.entity.User;
import com.smartemergency.exception.BusinessException;
import com.smartemergency.repository.HospitalRepository;
import com.smartemergency.repository.UserRepository;
import com.smartemergency.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service - Handles register, login, token refresh.
 * Login bypasses AuthenticationManager for compatibility with
 * both BCrypt and plain-text legacy passwords.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // NOTE: AuthenticationManager removed intentionally.
    // We do manual validation to support mixed password formats.

    // =====================================================
    // REGISTER
    // =====================================================

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                "Email already registered: " + request.getEmail()
            );
        }

        // Check duplicate phone
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(
                "Phone number already registered: " + request.getPhone()
            );
        }

        // Build and save user
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
        log.info("New user registered: {} | Role: {}", savedUser.getEmail(), savedUser.getRole());

        String accessToken  = jwtService.generateToken(savedUser);
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

    // =====================================================
    // LOGIN
    // =====================================================

    @Transactional
    public AuthResponse login(LoginRequest request) {
    // Step 1: Find user by email
    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

    // Step 2: Check if account is active
    if (!Boolean.TRUE.equals(user.getIsActive())) {
        throw new BusinessException("Account is deactivated. Please contact support.");
    }

    // Step 3: Manual password validation (bypasses AuthenticationManager)
    boolean passwordMatches = false;

    // Check BCrypt hash
    try {
        passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
    } catch (Exception e) {
        // ignore bcrypt error
    }

    // Fallback: plain text comparison for old users
    if (!passwordMatches) {
        passwordMatches = request.getPassword().equals(user.getPassword());
    }

    if (!passwordMatches) {
        throw new BadCredentialsException("Invalid email or password");
    }

    // Step 4: Auto-upgrade plain text password to BCrypt
    if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

    // Step 5: Generate JWT tokens
    String accessToken  = jwtService.generateToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    // Step 6: Get hospitalId if hospital role
    Long hospitalId = null;
    try {
        if (user.getRole() == com.smartemergency.enums.Role.HOSPITAL) {
            hospitalId = hospitalRepository.findByUser(user)
                    .map(h -> h.getId()).orElse(null);
        }
    } catch (Exception e) {
        // ignore
    }

    log.info("User logged in successfully: {}", user.getEmail());

    return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .userId(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .hospitalId(hospitalId)
            .message("Login successful!")
            .build();
}

    // =====================================================
    // REFRESH TOKEN
    // =====================================================

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