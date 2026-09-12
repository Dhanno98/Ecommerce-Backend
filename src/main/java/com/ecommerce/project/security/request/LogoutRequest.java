package com.ecommerce.project.security.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {

    @NotBlank
    private String refreshToken;
}
