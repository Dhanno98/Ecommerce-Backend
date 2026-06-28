package com.ecommerce.project.util;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.services.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUtilTest {

    @InjectMocks
    AuthUtil authUtil;

    @Mock
    UserRepository userRepository;

    /// loggedInEmail()
    @Test
    void loggedInEmailShouldReturnTheEmailOfTheLoggedInUser() {
        Role role = new Role(AppRole.ROLE_USER);

        User user = createUser(1L);
        user.setRoles(new HashSet<>());
        user.getRoles().add(role);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            String result = authUtil.loggedInEmail();
            assertNotNull(result);
            assertEquals(user.getEmail(), result);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /// loggedInUserId()
    @Test
    void loggedInUserIdShouldReturnTheUserIdOfTheLoggedInUser() {
        Role role = new Role(AppRole.ROLE_USER);

        User user = createUser(1L);
        user.setRoles(new HashSet<>());
        user.getRoles().add(role);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            Long result = authUtil.loggedInUserId();
            assertNotNull(result);
            assertEquals(user.getUserId(), result);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /// loggedInUser()
    @Test
    void loggedInUserShouldReturnTheLoggedInUser() {
        Role role = new Role(AppRole.ROLE_USER);

        User user = createUser(1L);
        user.setRoles(new HashSet<>());
        user.getRoles().add(role);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findById(userDetails.getId()))
                .thenReturn(Optional.of(user));

        try {
            User result = authUtil.loggedInUser();
            assertNotNull(result);
            assertSame(user, result);

            verify(userRepository).findById(userDetails.getId());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void loggedInUserShouldThrowResourceNotFoundExceptionIfUserNotFoundInDB() {
        Role role = new Role(AppRole.ROLE_USER);

        User user = createUser(1L);
        user.setRoles(new HashSet<>());
        user.getRoles().add(role);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findById(userDetails.getId()))
                .thenReturn(Optional.empty());

        try {
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> authUtil.loggedInUser()
            );
            assertEquals("User not found with userId: " + userDetails.getId(), exception.getMessage());

            verify(userRepository).findById(userDetails.getId());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /// HELPERS
    private User createUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName("Test User");
        user.setEmail("user@gmail.com");
        user.setPassword("password");
        return user;
    }
}