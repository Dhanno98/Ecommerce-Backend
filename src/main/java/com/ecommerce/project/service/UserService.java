package com.ecommerce.project.service;

import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.AuthResponseWrapper;

public interface UserService {
    AuthResponseWrapper authenticateUser(LoginRequest loginRequest);

    void registerUser(SignupRequest signupRequest);
}

