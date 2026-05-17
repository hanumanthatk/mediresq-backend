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
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service
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
     * REGISTER
     */

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        /* CHECK EMAIL */

        if(userRepository.existsByEmail(request.getEmail())){

            throw new BusinessException(

                "Email already registered: " +

                request.getEmail()

            );

        }

        /* CHECK PHONE */

        if(userRepository.existsByPhone(request.getPhone())){

            throw new BusinessException(

                "Phone number already registered: " +

                request.getPhone()

            );

        }

        /* CREATE USER */

        User user = User.builder()

                .firstName(request.getFirstName())

                .lastName(request.getLastName())

                .email(request.getEmail())

                .phone(request.getPhone())

                .password(

                    passwordEncoder.encode(
                        request.getPassword()
                    )

                )

                .role(request.getRole())

                .address(request.getAddress())

                .bloodGroup(request.getBloodGroup())

                .emergencyContact(
                    request.getEmergencyContact()
                )

                .isActive(true)

                .isVerified(false)

                .build();

        User savedUser = userRepository.save(user);

        log.info(

            "New user registered: {} with role: {}",

            savedUser.getEmail(),

            savedUser.getRole()

        );

        /* TOKENS */

        String accessToken =

            jwtService.generateToken(savedUser);

        String refreshToken =

            jwtService.generateRefreshToken(savedUser);

        return AuthResponse.builder()

                .accessToken(accessToken)

                .refreshToken(refreshToken)

                .tokenType("Bearer")

                .userId(savedUser.getId())

                .email(savedUser.getEmail())

                .fullName(savedUser.getFullName())

                .role(savedUser.getRole())

                .message(

                    "Registration successful!"

                )

                .build();

    }

    /**
     * LOGIN
     */

    public AuthResponse login(LoginRequest request) {

        /* FIND USER */

        User user = userRepository

                .findByEmail(request.getEmail())

                .orElseThrow(() ->

                    new BusinessException(

                        "Invalid email or password"

                    )

                );

        /* CHECK ACTIVE */

        if(!Boolean.TRUE.equals(user.getIsActive())){

            throw new BusinessException(

                "Account is deactivated"

            );

        }

        boolean validPassword = false;

        /* NEW BCRYPT PASSWORD */

        try{

            if(passwordEncoder.matches(

                request.getPassword(),

                user.getPassword()

            )){

                validPassword = true;

            }

        }catch(Exception e){

            log.warn(

                "Bcrypt check failed for user: {}",

                user.getEmail()

            );

        }

        /* OLD PLAIN TEXT PASSWORD */

        if(!validPassword){

            if(user.getPassword().equals(

                request.getPassword()

            )){

                validPassword = true;

                /* AUTO CONVERT TO BCRYPT */

                String encodedPassword =

                    passwordEncoder.encode(

                        request.getPassword()

                    );

                user.setPassword(encodedPassword);

                userRepository.save(user);

                log.info(

                    "Password upgraded to bcrypt for user: {}",

                    user.getEmail()

                );

            }

        }

        /* INVALID */

        if(!validPassword){

            throw new BusinessException(

                "Invalid email or password"

            );

        }

        /* GENERATE TOKENS */

        String accessToken =

            jwtService.generateToken(user);

        String refreshToken =

            jwtService.generateRefreshToken(user);

        log.info(

            "User logged in successfully: {}",

            user.getEmail()

        );

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
     * REFRESH TOKEN
     */

    public AuthResponse refreshToken(

        String refreshToken

    ){

        String email =

            jwtService.extractUsername(
                refreshToken
            );

        User user = userRepository

                .findByEmail(email)

                .orElseThrow(() ->

                    new BusinessException(

                        "Invalid refresh token"

                    )

                );

        if(!jwtService.isTokenValid(

            refreshToken,

            user

        )){

            throw new BusinessException(

                "Refresh token expired"

            );

        }

        String newAccessToken =

            jwtService.generateToken(user);

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