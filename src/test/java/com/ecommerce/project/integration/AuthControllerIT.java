package com.ecommerce.project.integration;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.PromoteRoleRequestDTO;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.services.UserDetailsImpl;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;

import static com.ecommerce.project.model.AppRole.ROLE_ADMIN;
import static com.ecommerce.project.model.AppRole.ROLE_SELLER;
import static com.ecommerce.project.model.AppRole.ROLE_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /// login()
    @Test
    void loginShouldSuccessfullyLoginUserWithValidCredentials() throws Exception {
        Role savedRole = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.setPassword(passwordEncoder.encode("password"));
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        LoginRequest loginRequest = createLoginRequest("Test User", "password");
        String json = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value(savedUser.getEmail()))
                .andExpect(jsonPath("$.id").value(savedUser.getUserId()))
                .andExpect(jsonPath("$.jwtToken").isNotEmpty())
                .andExpect(jsonPath("$.jwtToken").isString())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value(ROLE_USER.name()))
                .andExpect(jsonPath("$.username").value(savedUser.getUserName()));
    }

    @Test
    void loginShouldReturnUnauthorizedIfUserTriesToLoginWithInvalidCredentials() throws Exception {
        Role savedRole = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.setPassword(passwordEncoder.encode("password"));
        user.getRoles().add(savedRole);
        userRepository.save(user);

        LoginRequest loginRequest = createLoginRequest("Test User", "userPass");
        String json = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginShouldReturnBadRequestIfLoginRequestIsInvalid() throws Exception {
        LoginRequest loginRequest = createLoginRequest("", "");
        String json = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("must not be blank"))
                .andExpect(jsonPath("$.password").value("must not be blank"));
    }

    /// registerUser()
    @Test
    void registerUserShouldSuccessfullyRegisterANewUser() throws Exception {
        Role roleUser = roleRepository.findByRoleName(ROLE_USER).orElseThrow();
        Role roleSeller = roleRepository.findByRoleName(ROLE_SELLER).orElseThrow();
        Role roleAdmin = roleRepository.findByRoleName(ROLE_ADMIN).orElseThrow();

        SignupRequest signupRequest = createSignupRequest("Test User", "user@gmail.com", "password");
        String json = objectMapper.writeValueAsString(signupRequest);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.username").value(signupRequest.getUsername().trim().toLowerCase()))
                .andExpect(jsonPath("$.email").value(signupRequest.getEmail().trim().toLowerCase()))
                .andExpect(jsonPath("$.message").value("User registered successfully!"));

        User userFromDB = userRepository.findByUserName("test user").orElseThrow();
        assertEquals(signupRequest.getUsername().trim().toLowerCase(), userFromDB.getUserName());
        assertEquals(signupRequest.getEmail(), userFromDB.getEmail());

        assertEquals(1, userFromDB.getRoles().size());
        assertTrue(userFromDB.getRoles().contains(roleUser));
        assertFalse(userFromDB.getRoles().contains(roleSeller));
        assertFalse(userFromDB.getRoles().contains(roleAdmin));
    }

    @Test
    void registerUserShouldReturnBadRequestIfUsernameIsAlreadyTaken() throws Exception {
        Role savedRole = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User user = createUser("test user", "user@gmail.com", "password");
        user.setPassword(passwordEncoder.encode("password"));
        user.getRoles().add(savedRole);
        userRepository.save(user);

        SignupRequest signupRequest = createSignupRequest("Test User", "testuser@gmail.com", "password");
        String json = objectMapper.writeValueAsString(signupRequest);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"))
                .andExpect(jsonPath("$.status").value(false));

        assertEquals(1, userRepository.count());
    }

    @Test
    void registerUserShouldReturnBadRequestIfEmailIsAlreadyInUse() throws Exception {
        Role savedRole = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User user = createUser("test user", "user@gmail.com", "password");
        user.setPassword(passwordEncoder.encode("password"));
        user.getRoles().add(savedRole);
        userRepository.save(user);

        SignupRequest signupRequest = createSignupRequest("New Test User", "user@gmail.com", "password");
        String json = objectMapper.writeValueAsString(signupRequest);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Error: Email is already in use!"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void registerUserShouldReturnBadRequestIfSignupRequestIsInvalid() throws Exception {
        SignupRequest signupRequest = createSignupRequest("", "", "");
        String json = objectMapper.writeValueAsString(signupRequest);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    /// currentUserName()
    @Test
    void currentUserNameShouldReturnTheUsernameOfTheLoggedInUser() throws Exception {
        Role savedRole = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User user = createUser("test user", "user@gmail.com", "password");
        user.setPassword(passwordEncoder.encode("password"));
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/auth/username")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string(savedUser.getUserName()));
    }

    @Test
    void currentUserNameShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/username"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// getUserDetails()
    @Test
    void getUserDetailsShouldSuccessfullyReturnUserDetailsOfTheLoggedInUser() throws Exception {
        Role savedRole = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User user = createUser("test user", "user@gmail.com", "password");
        user.setPassword(passwordEncoder.encode("password"));
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/auth/user")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value(savedUser.getEmail()))
                .andExpect(jsonPath("$.id").value(savedUser.getUserId()))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value(ROLE_USER.name()))
                .andExpect(jsonPath("$.username").value(savedUser.getUserName()));
    }

    @Test
    void getUserDetailsShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// signoutUser()
    @Test
    void signoutUserShouldSuccessfullySignoutTheLoggedInUser() throws Exception {
        Role savedRole = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User user = createUser("test user", "user@gmail.com", "password");
        user.setPassword(passwordEncoder.encode("password"));
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(post("/api/auth/signout")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("You've been signed out!"));
    }

    @Test
    void signoutUserShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/auth/signout"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// getAllSellers()
    @Test
    void getAllSellersShouldReturnAllSellers() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(ROLE_ADMIN).orElseThrow();
        Role roleSeller = roleRepository.findByRoleName(ROLE_SELLER).orElseThrow();
        Role roleUser = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User admin = createUser("Test Admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        User seller = createUser("Test Seller", "seller@gmail.com", "password");
        seller.getRoles().add(roleSeller);
        User savedSeller = userRepository.save(seller);

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        userRepository.save(user);

        mockMvc.perform(get("/api/auth/admin/sellers")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(savedSeller.getUserId()))
                .andExpect(jsonPath("$.content[0].username").value(savedSeller.getUserName()))
                .andExpect(jsonPath("$.content[0].email").value(savedSeller.getEmail()))
                .andExpect(jsonPath("$.content[0].addresses").isEmpty())
                .andExpect(jsonPath("$.content[0].roles.length()").value(1))
                .andExpect(jsonPath("$.content[0].roles[0]").value(ROLE_SELLER.name()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void getAllSellersShouldReturnEmptyPageIfNoSellerExists() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(ROLE_ADMIN).orElseThrow();
        Role roleUser = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User admin = createUser("Test Admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        userRepository.save(user);

        mockMvc.perform(get("/api/auth/admin/sellers")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void getAllSellersShouldReturnBadRequestIfPaginationParametersAreInvalid() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(ROLE_ADMIN).orElseThrow();

        User admin = createUser("Test Admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        mockMvc.perform(get("/api/auth/admin/sellers")
                        .with(user(userDetails))
                        .param("sortOrder", "ascending"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid sort order"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void getAllSellersShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
        Role roleSeller = roleRepository.findByRoleName(ROLE_SELLER).orElseThrow();

        User seller = createUser("Test Admin", "admin@gmail.com", "password");
        seller.getRoles().add(roleSeller);
        User savedSeller = userRepository.save(seller);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller);

        mockMvc.perform(get("/api/auth/admin/sellers")
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getAllSellersShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(ROLE_ADMIN).orElseThrow();

        User admin = createUser("Test Admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        userRepository.save(admin);

        mockMvc.perform(get("/api/auth/admin/sellers"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// promoteUser()
    @Test
    void promoteUserShouldSuccessfullyPromoteUser() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(ROLE_ADMIN).orElseThrow();
        Role roleSeller = roleRepository.findByRoleName(ROLE_SELLER).orElseThrow();
        Role roleUser = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User admin = createUser("test admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        User user = createUser("test user", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        PromoteRoleRequestDTO requestDTO = new PromoteRoleRequestDTO();
        requestDTO.setRole(ROLE_SELLER.name());
        String json = objectMapper.writeValueAsString(requestDTO);

        Long userId = savedUser.getUserId();

        mockMvc.perform(post("/api/auth/admin/users/{userId}/promote", userId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User promoted successfully!"));

        User userFromDB = userRepository.findByUserName("test user").orElseThrow();
        assertTrue(userFromDB.getRoles().contains(roleUser));
        assertTrue(userFromDB.getRoles().contains(roleSeller));
        assertFalse(userFromDB.getRoles().contains(roleAdmin));
        assertEquals(2, userFromDB.getRoles().size());
    }

    @Test
    void promoteUserShouldReturnNotFoundIfUserDoesNotExist() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(ROLE_ADMIN).orElseThrow();

        User admin = createUser("test admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        PromoteRoleRequestDTO requestDTO = new PromoteRoleRequestDTO();
        requestDTO.setRole(ROLE_SELLER.name());
        String json = objectMapper.writeValueAsString(requestDTO);

        Long userId = Long.MAX_VALUE;

        mockMvc.perform(post("/api/auth/admin/users/{userId}/promote", userId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User not found with userId: " + userId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void promoteUserShouldReturnBadRequestIfRoleSuppliedForPromotionIsInvalid() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(ROLE_ADMIN).orElseThrow();
        Role roleUser = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User admin = createUser("test admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        User user = createUser("test user", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        PromoteRoleRequestDTO requestDTO = new PromoteRoleRequestDTO();
        requestDTO.setRole("ROLE_MANAGER");
        String json = objectMapper.writeValueAsString(requestDTO);

        Long userId = savedUser.getUserId();

        mockMvc.perform(post("/api/auth/admin/users/{userId}/promote", userId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid role: " + requestDTO.getRole()))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void promoteUserShouldReturnBadRequestIfRoleSuppliedForPromotionIsRoleUser() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(ROLE_ADMIN).orElseThrow();
        Role roleUser = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User admin = createUser("test admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        User user = createUser("test user", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        PromoteRoleRequestDTO requestDTO = new PromoteRoleRequestDTO();
        requestDTO.setRole(ROLE_USER.name());
        String json = objectMapper.writeValueAsString(requestDTO);

        Long userId = savedUser.getUserId();

        mockMvc.perform(post("/api/auth/admin/users/{userId}/promote", userId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Users already have ROLE_USER"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void promoteUserShouldReturnBadRequestIfUserAlreadyHasRoleSuppliedInTheRequest() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(ROLE_ADMIN).orElseThrow();
        Role roleUser = roleRepository.findByRoleName(ROLE_USER).orElseThrow();

        User admin = createUser("test admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        User user = createUser("test user", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        user.getRoles().add(roleAdmin);
        User savedUser = userRepository.save(user);

        PromoteRoleRequestDTO requestDTO = new PromoteRoleRequestDTO();
        requestDTO.setRole(ROLE_ADMIN.name());
        String json = objectMapper.writeValueAsString(requestDTO);

        Long userId = savedUser.getUserId();

        mockMvc.perform(post("/api/auth/admin/users/{userId}/promote", userId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User already has role: " + requestDTO.getRole()))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void promoteUserShouldReturnBadRequestIfPromoteRoleRequestDTOIsInvalid() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(ROLE_ADMIN).orElseThrow();

        User admin = createUser("test admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        PromoteRoleRequestDTO requestDTO = new PromoteRoleRequestDTO();
        requestDTO.setRole("");
        String json = objectMapper.writeValueAsString(requestDTO);

        Long userId = 1L;

        mockMvc.perform(post("/api/auth/admin/users/{userId}/promote", userId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.role").value("must not be blank"));
    }

    @Test
    void promoteUserShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
        Role roleSeller = roleRepository.findByRoleName(ROLE_SELLER).orElseThrow();

        User seller = createUser("test admin", "admin@gmail.com", "password");
        seller.getRoles().add(roleSeller);
        User savedSeller = userRepository.save(seller);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller);

        PromoteRoleRequestDTO requestDTO = new PromoteRoleRequestDTO();
        requestDTO.setRole(ROLE_ADMIN.name());
        String json = objectMapper.writeValueAsString(requestDTO);

        Long userId = 1L;

        mockMvc.perform(post("/api/auth/admin/users/{userId}/promote", userId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void promoteUserShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        PromoteRoleRequestDTO requestDTO = new PromoteRoleRequestDTO();
        requestDTO.setRole(ROLE_ADMIN.name());
        String json = objectMapper.writeValueAsString(requestDTO);

        Long userId = 1L;

        mockMvc.perform(post("/api/auth/admin/users/{userId}/promote", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// HELPERS
    private User createUser(String userName, String email, String password) {
        User user = new User();
        user.setUserName(userName);
        user.setEmail(email);
        user.setPassword(password);
        user.setRoles(new HashSet<>());
        return user;
    }

    private LoginRequest createLoginRequest(String name, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(name);
        loginRequest.setPassword(password);
        return loginRequest;
    }

    private SignupRequest createSignupRequest(String username, String email, String password) {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(username);
        signupRequest.setEmail(email);
        signupRequest.setPassword(password);
        return signupRequest;
    }
}
