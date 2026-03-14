package com.ecommerce.project.service;

import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.UserInfoResponse;

public interface UserService {
    UserInfoResponse authenticateUser(LoginRequest loginRequest);

    void registerUser(SignupRequest signupRequest);
}

