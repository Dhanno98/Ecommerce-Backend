package com.ecommerce.project.controller;

import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.AuthResponseWrapper;
import com.ecommerce.project.security.response.MessageResponse;
import com.ecommerce.project.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponseWrapper authWrapper = userService.authenticateUser(loginRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authWrapper.getJwtCookie().toString())
                .body(authWrapper.getUserInfoResponse());
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        userService.registerUser(signupRequest);
        return new ResponseEntity<>(new MessageResponse("User registered successfully!"), HttpStatus.OK);
    }
}
