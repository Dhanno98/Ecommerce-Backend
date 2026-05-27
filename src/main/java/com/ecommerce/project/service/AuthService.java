package com.ecommerce.project.service;

import com.ecommerce.project.payload.PromoteRoleRequestDTO;
import com.ecommerce.project.payload.SellerResponse;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.UserInfoResponse;
import org.springframework.security.core.Authentication;

public interface AuthService {
    UserInfoResponse login(LoginRequest loginRequest);

    void register(SignupRequest signupRequest);

    String currentUserName(Authentication authentication);

    UserInfoResponse getCurrentUserDetails(Authentication authentication);

//    AuthenticationResult logoutUser();

    SellerResponse getAllSellers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    void promoteUser(Long userId, PromoteRoleRequestDTO requestDTO);
}

