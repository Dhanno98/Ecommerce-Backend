package com.ecommerce.project.service;

import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.AuthResponseWrapper;
import com.ecommerce.project.security.response.UserInfoResponse;
import org.springframework.security.core.Authentication;

public interface AuthService {
    AuthResponseWrapper authenticateUser(LoginRequest loginRequest);

    void registerUser(SignupRequest signupRequest);

    String currentUserName(Authentication authentication);

    UserInfoResponse getUserDetails(Authentication authentication);

    AuthResponseWrapper signoutUser();
}

