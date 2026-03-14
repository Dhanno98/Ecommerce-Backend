package com.ecommerce.project.security.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseCookie;

@Data
@AllArgsConstructor
public class AuthResponseWrapper {
    private UserInfoResponse userInfoResponse;
    private ResponseCookie jwtCookie;
}
