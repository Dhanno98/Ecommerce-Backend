package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.RefreshToken;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.RefreshTokenRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.response.RefreshTokenResponse;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final UserRepository userRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    private final JwtUtils jwtUtils;

    private final AuthUtil authUtil;

    @Value("${spring.app.refreshTokenExpirationMs}")
    private long refreshTokenExpirationMs;

    @Override
    public String generateRefreshToken(String username) {
        String rawToken = generateRawToken();

        String tokenHash = hashRawToken(rawToken);

        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Instant createdAt = Instant.now();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setUser(user);
        refreshToken.setCreatedAt(createdAt);
        refreshToken.setExpiryDate(createdAt.plusMillis(refreshTokenExpirationMs));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Override
    public RefreshTokenResponse rotateRefreshToken(String rawRefreshToken) {
        String tokenHash = hashRawToken(rawRefreshToken);

        RefreshToken refreshTokenFromDB = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new APIException("Refresh token is invalid!"));

        if (refreshTokenFromDB.isRevoked()) {
            throw new APIException("Refresh Token has already been revoked!");
        }

        if (!refreshTokenFromDB.getExpiryDate().isAfter(Instant.now())) {
            throw new APIException("Refresh Token has expired. Please login again.");
        }

        User user = refreshTokenFromDB.getUser();
        String username = user.getUserName();

        String newJwtToken = jwtUtils.generateTokenFromUsername(username);

        refreshTokenFromDB.setRevoked(true);
        String newRefreshToken = generateRefreshToken(username);

        return new RefreshTokenResponse(newJwtToken, newRefreshToken);
    }

    @Override
    public void revokeRefreshToken(String rawRefreshToken) {
        String tokenHash = hashRawToken(rawRefreshToken);

        RefreshToken refreshTokenFromDB = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new APIException("Refresh token is invalid!"));

        if (refreshTokenFromDB.isRevoked() || !refreshTokenFromDB.getExpiryDate().isAfter(Instant.now())) {
            return;
        }

        refreshTokenFromDB.setRevoked(true);
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashRawToken(String rawToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
