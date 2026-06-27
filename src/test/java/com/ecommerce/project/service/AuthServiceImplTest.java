package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.payload.PromoteRoleRequestDTO;
import com.ecommerce.project.payload.SellerDTO;
import com.ecommerce.project.payload.SellerResponse;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.UserInfoResponse;
import com.ecommerce.project.security.services.UserDetailsImpl;
import com.ecommerce.project.util.PaginationValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @InjectMocks
    AuthServiceImpl authService;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtUtils jwtUtils;

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    PaginationValidator paginationValidator;

    @Mock
    AddressRepository addressRepository;

    @Mock
    ModelMapper modelMapper;

    /// login()
    @Test
    void loginShouldReturnUserInfoWhenCredentialsAreValid() {
        User user = createUser(1L);

        Role role1 = new Role();
        role1.setRoleName(AppRole.ROLE_USER);

        Role role2 = new Role();
        role2.setRoleName(AppRole.ROLE_ADMIN);

        user.setRoles(Set.of(role1, role2));

        LoginRequest loginRequest = createLoginRequest("Test User", "password");

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities()
        );

        String jwtToken = "dummyJwtToken";

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        when(jwtUtils.generateTokenFromUsername(userDetails.getUsername()))
                .thenReturn(jwtToken);

        UserInfoResponse result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals(user.getUserId(), result.getId());
        assertEquals(user.getUserName(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());

        assertEquals(jwtToken, result.getJwtToken());
        assertIterableEquals(List.of("ROLE_ADMIN", "ROLE_USER"), result.getRoles().stream().sorted().toList());

        try {
            assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
        } finally {
            SecurityContextHolder.clearContext();
        }

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) captor.getValue();
        assertEquals(loginRequest.getUsername(), token.getPrincipal());
        assertEquals(loginRequest.getPassword(), token.getCredentials());

        verify(jwtUtils).generateTokenFromUsername(userDetails.getUsername());
    }

    @Test
    void loginShouldThrowBadCredentialsExceptionWhenCredentialsAreInvalid() {
        LoginRequest loginRequest = createLoginRequest("Test User 2", "password2");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.clearContext();

        verify(authenticationManager).authenticate(any(Authentication.class));
        verifyNoInteractions(jwtUtils);
    }

    /// register()
    @Test
    void registerShouldSuccessfullyRegisterAUser() {
        SignupRequest signupRequest = createSignupRequest("Test User", "user@gmail.com", "password");

        String normalizedUsername = signupRequest.getUsername().trim().toLowerCase();
        String normalizedEmail = signupRequest.getEmail().trim().toLowerCase();

        Role role = new Role(AppRole.ROLE_USER);

        String encodedPassword = "!@#ui%$q";

        when(userRepository.existsByUserName(normalizedUsername))
                .thenReturn(false);

        when(userRepository.existsByEmail(normalizedEmail))
                .thenReturn(false);

        when(passwordEncoder.encode(signupRequest.getPassword()))
                .thenReturn(encodedPassword);

        when(roleRepository.findByRoleName(AppRole.ROLE_USER))
                .thenReturn(Optional.of(role));

        authService.register(signupRequest);

        verify(userRepository).existsByUserName(signupRequest.getUsername().trim().toLowerCase());
        verify(userRepository).existsByEmail(signupRequest.getEmail().trim().toLowerCase());
        verify(passwordEncoder).encode(signupRequest.getPassword());
        verify(roleRepository).findByRoleName(AppRole.ROLE_USER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();

        assertEquals(normalizedUsername, savedUser.getUserName());
        assertEquals(normalizedEmail, savedUser.getEmail());
        assertEquals(encodedPassword, savedUser.getPassword());
        assertEquals(Set.of(role), savedUser.getRoles());
    }

    @Test
    void registerShouldThrowApiExceptionWhenUsernameAlreadyExists() {
        SignupRequest signupRequest = createSignupRequest("Test User", "user@gmail.com", "password");

        String normalizedUsername = signupRequest.getUsername().trim().toLowerCase();
        String normalizedEmail = signupRequest.getEmail().trim().toLowerCase();

        when(userRepository.existsByUserName(normalizedUsername))
                .thenReturn(true);

        APIException exception = assertThrows(
                APIException.class,
                () -> authService.register(signupRequest)
        );

        assertEquals("Error: Username is already taken!", exception.getMessage());

        verify(userRepository).existsByUserName(normalizedUsername);
        verify(userRepository, never()).existsByEmail(normalizedEmail);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(roleRepository);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerShouldThrowApiExceptionWhenEmailAlreadyExists() {
        SignupRequest signupRequest = createSignupRequest("Test User", "user@gmail.com", "password");

        String normalizedUsername = signupRequest.getUsername().trim().toLowerCase();
        String normalizedEmail = signupRequest.getEmail().trim().toLowerCase();

        when(userRepository.existsByUserName(normalizedUsername))
                .thenReturn(false);

        when(userRepository.existsByEmail(normalizedEmail))
                .thenReturn(true);

        APIException exception = assertThrows(
                APIException.class,
                () -> authService.register(signupRequest)
        );

        assertEquals("Error: Email is already in use!", exception.getMessage());

        verify(userRepository).existsByUserName(normalizedUsername);
        verify(userRepository).existsByEmail(normalizedEmail);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(roleRepository);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerShouldThrowApiExceptionWhenDefaultRoleIsMissing() {
        SignupRequest signupRequest = createSignupRequest("Test User", "user@gmail.com", "password");

        String normalizedUsername = signupRequest.getUsername().trim().toLowerCase();
        String normalizedEmail = signupRequest.getEmail().trim().toLowerCase();

        String encodedPassword = "!@#ui%$q";

        when(userRepository.existsByUserName(normalizedUsername))
                .thenReturn(false);

        when(userRepository.existsByEmail(normalizedEmail))
                .thenReturn(false);

        when(passwordEncoder.encode(signupRequest.getPassword()))
                .thenReturn(encodedPassword);

        when(roleRepository.findByRoleName(AppRole.ROLE_USER))
                .thenReturn(Optional.empty());

        APIException exception = assertThrows(
                APIException.class,
                () -> authService.register(signupRequest)
        );

        assertEquals("Error: Role not found", exception.getMessage());

        verify(userRepository).existsByUserName(normalizedUsername);
        verify(userRepository).existsByEmail(normalizedEmail);
        verify(passwordEncoder).encode(signupRequest.getPassword());
        verify(roleRepository).findByRoleName(AppRole.ROLE_USER);
        verify(userRepository, never()).save(any(User.class));
    }

    /// currentUserName()
    @Test
    void currentUserNameShouldReturnTheUsernameOfALoggedInUser() {
        User user = createUser(1L);

        Role role1 = new Role();
        role1.setRoleName(AppRole.ROLE_USER);

        Role role2 = new Role();
        role2.setRoleName(AppRole.ROLE_ADMIN);

        user.setRoles(Set.of(role1, role2));

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities()
        );

        String result = authService.currentUserName(authentication);

        assertNotNull(result);
        assertEquals(user.getUserName(), result);
    }

    /// getAllSellers()
    @Test
    void getAllSellersShouldReturnAllSellers() {
        Role roleUser = new Role(AppRole.ROLE_USER);
        Role roleSeller = new Role(AppRole.ROLE_SELLER);
        Role roleAdmin = new Role(AppRole.ROLE_ADMIN);

        User seller1 = createSeller(1L, "Test Seller 1");
        seller1.setRoles(Set.of(roleUser, roleSeller));

        User seller2 = createSeller(2L, "Test Seller 2");
        seller2.setRoles(Set.of(roleSeller, roleAdmin));

        Address address1 = createAddress("123 Maple Street, Apt 4B", "Oakwood Commons", "New York", "NY", "USA", "62704");
        address1.setAddressId(1L);

        Address address2 = createAddress("123 Orange Street, Apt 4B", "Greenwood Commons", "Los Angeles", "CA", "USA", "90001");
        address2.setAddressId(2L);

        seller1.setAddresses(List.of(address1));
        address1.setUser(seller1);

        seller2.setAddresses(List.of(address2));
        address2.setUser(seller2);

        List<User> sellers = List.of(seller1, seller2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> sellerPage = new PageImpl<>(sellers, pageable, sellers.size());

        List<Address> addresses = List.of(address1, address2);

        List<Long> sellerIds = List.of(1L, 2L);

        AddressDTO addressDTO1 = createAddressDTO(address1);
        AddressDTO addressDTO2 = createAddressDTO(address2);

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(userRepository.findByRoleName(eq(AppRole.ROLE_SELLER), any(Pageable.class)))
                .thenReturn(sellerPage);

        when(addressRepository.findByUserIds(sellerIds))
                .thenReturn(addresses);

        when(modelMapper.map(address1, AddressDTO.class))
                .thenReturn(addressDTO1);

        when(modelMapper.map(address2, AddressDTO.class))
                .thenReturn(addressDTO2);

        when(userRepository.findUsersWithRoles(sellerIds))
                .thenReturn(sellers);

        SellerResponse result = authService.getAllSellers(0, 10, "userId", "asc");

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        SellerDTO returnedSeller1 = result.getContent().getFirst();
        assertEquals(seller1.getUserId(), returnedSeller1.getUserId());
        assertEquals(seller1.getUserName(), returnedSeller1.getUsername());
        assertEquals(seller1.getEmail(), returnedSeller1.getEmail());
        assertEquals(List.of(addressDTO1), returnedSeller1.getAddresses());
        assertEquals(Set.of("ROLE_USER", "ROLE_SELLER"), returnedSeller1.getRoles());

        SellerDTO returnedSeller2 = result.getContent().get(1);
        assertEquals(seller2.getUserId(), returnedSeller2.getUserId());
        assertEquals(seller2.getUserName(), returnedSeller2.getUsername());
        assertEquals(seller2.getEmail(), returnedSeller2.getEmail());
        assertEquals(List.of(addressDTO2), returnedSeller2.getAddresses());
        assertEquals(Set.of("ROLE_ADMIN", "ROLE_SELLER"), returnedSeller2.getRoles());

        verify(paginationValidator).validate(eq(0), eq(10), eq("userId"), eq("asc"), anyList());
        verify(userRepository).findByRoleName(eq(AppRole.ROLE_SELLER), any(Pageable.class));
        verify(addressRepository).findByUserIds(sellerIds);
        verify(modelMapper).map(address1, AddressDTO.class);
        verify(modelMapper).map(address2, AddressDTO.class);
        verify(userRepository).findUsersWithRoles(sellerIds);
    }

    /// promoteUser()
    @Test
    void promoteUserShouldSuccessfullyPromoteUser() {
        Role roleUser = new Role(AppRole.ROLE_USER);

        User user = createUser(1L);
        user.setRoles(new HashSet<>());
        user.getRoles().add(roleUser);

        PromoteRoleRequestDTO roleRequestDTO = new PromoteRoleRequestDTO();
        roleRequestDTO.setRole("ROLE_ADMIN");

        AppRole appRole = AppRole.valueOf(roleRequestDTO.getRole().trim().toUpperCase());
        Role newRole = new Role(appRole);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByRoleName(appRole))
                .thenReturn(Optional.of(newRole));

        authService.promoteUser(1L, roleRequestDTO);

        assertTrue(user.getRoles().contains(newRole));
        assertTrue(user.getRoles().contains(roleUser));
        assertEquals(2, user.getRoles().size());

        verify(userRepository).findById(1L);
        verify(roleRepository).findByRoleName(appRole);
        verify(userRepository).save(user);
    }

    @Test
    void promoteUserShouldThrowResourceNotFoundExceptionWhenUserDoesNotExist() {
        PromoteRoleRequestDTO roleRequestDTO = new PromoteRoleRequestDTO();
        roleRequestDTO.setRole("ROLE_ADMIN");

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.promoteUser(1L, roleRequestDTO)
        );

        assertEquals("User not found with userId: 1", exception.getMessage());

        verify(userRepository).findById(1L);
        verifyNoInteractions(roleRepository);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void promoteUserShouldThrowApiExceptionIfRoleSuppliedIsInvalid() {
        Role roleUser = new Role(AppRole.ROLE_USER);

        User user = createUser(1L);
        user.setRoles(new HashSet<>());
        user.getRoles().add(roleUser);

        PromoteRoleRequestDTO roleRequestDTO = new PromoteRoleRequestDTO();
        roleRequestDTO.setRole("ROLE_MANAGER");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        APIException exception = assertThrows(
                APIException.class,
                () -> authService.promoteUser(1L, roleRequestDTO)
        );

        assertEquals("Invalid role: " + roleRequestDTO.getRole(), exception.getMessage());

        verify(userRepository).findById(1L);
        verifyNoInteractions(roleRepository);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void promoteUserShouldThrowApiExceptionIfDefaultRoleSupplied() {
        Role roleUser = new Role(AppRole.ROLE_USER);

        User user = createUser(1L);
        user.setRoles(new HashSet<>());
        user.getRoles().add(roleUser);

        PromoteRoleRequestDTO roleRequestDTO = new PromoteRoleRequestDTO();
        roleRequestDTO.setRole("ROLE_USER");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        APIException exception = assertThrows(
                APIException.class,
                () -> authService.promoteUser(1L, roleRequestDTO)
        );

        assertEquals("Users already have ROLE_USER", exception.getMessage());

        verify(userRepository).findById(1L);
        verifyNoInteractions(roleRepository);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void promoteUserShouldThrowApiExceptionIfRoleNotFound() {
        Role roleUser = new Role(AppRole.ROLE_USER);

        User user = createUser(1L);
        user.setRoles(new HashSet<>());
        user.getRoles().add(roleUser);

        PromoteRoleRequestDTO roleRequestDTO = new PromoteRoleRequestDTO();
        roleRequestDTO.setRole("ROLE_ADMIN");
        AppRole appRole = AppRole.valueOf(roleRequestDTO.getRole().trim().toUpperCase());

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByRoleName(appRole))
                .thenReturn(Optional.empty());

        APIException exception = assertThrows(
                APIException.class,
                () -> authService.promoteUser(1L, roleRequestDTO)
        );

        assertEquals("Role not found", exception.getMessage());

        verify(userRepository).findById(1L);
        verify(roleRepository).findByRoleName(appRole);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void promoteUserShouldThrowApiExceptionIfUserAlreadyHasThatRole() {
        Role roleUser = new Role(AppRole.ROLE_ADMIN);

        User user = createUser(1L);
        user.setRoles(new HashSet<>());
        user.getRoles().add(roleUser);

        PromoteRoleRequestDTO roleRequestDTO = new PromoteRoleRequestDTO();
        roleRequestDTO.setRole("ROLE_ADMIN");

        AppRole appRole = AppRole.valueOf(roleRequestDTO.getRole().trim().toUpperCase());
        Role newRole = new Role(appRole);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByRoleName(appRole))
                .thenReturn(Optional.of(newRole));

        APIException exception = assertThrows(
                APIException.class,
                () -> authService.promoteUser(1L, roleRequestDTO)
        );

        assertEquals("User already has role: " + appRole, exception.getMessage());

        verify(userRepository).findById(1L);
        verify(roleRepository).findByRoleName(appRole);
        verify(userRepository, never()).save(user);
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

    private LoginRequest createLoginRequest(String username, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
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

    private User createSeller(Long id, String name) {
        User seller = new User();
        seller.setUserId(id);
        seller.setUserName(name);
        seller.setEmail(name.toLowerCase().replaceAll("\\s", "") + "@gmail.com");
        seller.setPassword("password");
        return seller;
    }

    private Address createAddress(String street, String buildingName, String city, String state, String country, String pincode) {
        Address address = new Address();

        address.setStreet(street);
        address.setBuildingName(buildingName);
        address.setCity(city);
        address.setState(state);
        address.setCountry(country);
        address.setPincode(pincode);

        return address;
    }

    private AddressDTO createAddressDTO(Address address) {
        AddressDTO addressDTO = new AddressDTO();

        addressDTO.setAddressId(address.getAddressId());
        addressDTO.setStreet(address.getStreet());
        addressDTO.setBuildingName(address.getBuildingName());
        addressDTO.setCity(address.getCity());
        addressDTO.setState(address.getState());
        addressDTO.setCountry(address.getCountry());
        addressDTO.setPincode(address.getPincode());

        return addressDTO;
    }
}
