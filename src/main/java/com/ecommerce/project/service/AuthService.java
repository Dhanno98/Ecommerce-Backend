package com.ecommerce.project.service;

import com.ecommerce.project.payload.AuthenticationResult;
import com.ecommerce.project.payload.UserResponse;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.UserInfoResponse;
import org.springframework.security.core.Authentication;

public interface AuthService {
    AuthenticationResult login(LoginRequest loginRequest);

    void register(SignupRequest signupRequest);

    String currentUserName(Authentication authentication);

    UserInfoResponse getCurrentUserDetails(Authentication authentication);

    AuthenticationResult logoutUser();

    UserResponse getAllSellers(Integer pageNumber);
}

