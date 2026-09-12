package com.ecommerce.project.service;

import com.ecommerce.project.security.response.RefreshTokenResponse;

public interface RefreshTokenService {
    String generateRefreshToken(String username);

    RefreshTokenResponse rotateRefreshToken(String rawRefreshToken);

    void revokeRefreshToken(String rawRefreshToken);
}
