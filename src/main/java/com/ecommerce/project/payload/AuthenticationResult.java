package com.ecommerce.project.payload;

import com.ecommerce.project.security.response.UserInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseCookie;

@Data
@AllArgsConstructor
public class AuthenticationResult {
    private UserInfoResponse userInfoResponse;
    private ResponseCookie jwtCookie;
}
