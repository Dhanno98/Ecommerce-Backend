package com.ecommerce.project.controller;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.payload.PromoteRoleRequestDTO;
import com.ecommerce.project.payload.SellerResponse;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.MessageResponse;
import com.ecommerce.project.security.response.UserInfoResponse;
import com.ecommerce.project.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<UserInfoResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        UserInfoResponse response = authService.login(loginRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        authService.register(signupRequest);
        return new ResponseEntity<>(new MessageResponse("User registered successfully!"), HttpStatus.CREATED);
    }

    @GetMapping("/username")
    public String currentUserName(Authentication authentication) {
        return authService.currentUserName(authentication);
    }

    @GetMapping("/user")
    public ResponseEntity<UserInfoResponse> getUserDetails(Authentication authentication) {
        UserInfoResponse response = authService.getCurrentUserDetails(authentication);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/signout")
    public ResponseEntity<MessageResponse> signoutUser() {
        return ResponseEntity.ok().body(new MessageResponse("You've been signed out!"));
    }

    @GetMapping("/admin/sellers")
    public ResponseEntity<SellerResponse> getAllSellers(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_USERS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ) {
        SellerResponse userResponse = authService.getAllSellers(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @PostMapping("/admin/users/{userId}/promote")
    public ResponseEntity<MessageResponse> promoteUser(@PathVariable Long userId,
                                                       @Valid @RequestBody PromoteRoleRequestDTO requestDTO) {
        authService.promoteUser(userId, requestDTO);
        return new ResponseEntity<>(new MessageResponse("User promoted successfully!"), HttpStatus.OK);
    }

}
