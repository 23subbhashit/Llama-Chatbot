package com.aibot.llamachatbot.service;

import com.aibot.llamachatbot.dto.auth.*;
import com.aibot.llamachatbot.entity.RefreshToken;
import com.aibot.llamachatbot.entity.User;
import com.aibot.llamachatbot.exception.*;
import com.aibot.llamachatbot.repository.UserRepository;
import com.aibot.llamachatbot.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UnauthorizedException(
                    "Email already registered"
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(request.getPassword())
                )
                .build();

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Invalid email or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new UnauthorizedException(
                    "Invalid email or password"
            );
        }

        String accessToken =
                jwtService.generateAccessToken(user.getEmail());

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(
                        jwtService.getAccessTokenExpiration() / 1000
                )
                .build();
    }

    public AuthResponse refreshToken(
            RefreshTokenRequest request) {

        RefreshToken refreshToken =
                refreshTokenService.validateRefreshToken(
                        request.getRefreshToken()
                );

        User user = refreshToken.getUser();

        String newAccessToken =
                jwtService.generateAccessToken(user.getEmail());

        /*
         * Token rotation:
         * Revoke the old refresh token and create a new one.
         */
        refreshTokenService.revokeToken(
                refreshToken.getToken()
        );

        RefreshToken newRefreshToken =
                refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(
                        jwtService.getAccessTokenExpiration() / 1000
                )
                .build();
    }

    public void logout(RefreshTokenRequest request) {

        refreshTokenService.revokeToken(
                request.getRefreshToken()
        );
    }
}