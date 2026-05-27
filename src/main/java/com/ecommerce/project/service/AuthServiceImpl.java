package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.payload.PromoteRoleRequestDTO;
import com.ecommerce.project.payload.SellerDTO;
import com.ecommerce.project.payload.SellerResponse;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.UserInfoResponse;
import com.ecommerce.project.security.services.UserDetailsImpl;
import com.ecommerce.project.util.PaginationValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtUtils jwtUtils;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final ModelMapper modelMapper;

    private final PaginationValidator paginationValidator;

    private static final List<String> ALLOWED_SORT_FIELDS = List.of("userId", "userName", "email");

    @Override
    public UserInfoResponse login(LoginRequest loginRequest) throws AuthenticationException {
        Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String username = userDetails.getUsername();
        String jwtToken = jwtUtils.generateTokenFromUsername(username);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();

        return new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(), roles, userDetails.getEmail(), jwtToken);
    }

    @Override
    public void register(SignupRequest signupRequest) {
        String normalizedUsername = signupRequest.getUsername().trim().toLowerCase();
        String normalizedEmail = signupRequest.getEmail().trim().toLowerCase();

        if (userRepository.existsByUserName(normalizedUsername)) {
            throw new APIException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new APIException("Error: Email is already in use!");
        }

        User user = new User(
                normalizedUsername,
                normalizedEmail,
                passwordEncoder.encode(signupRequest.getPassword())
        );

        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                .orElseThrow(() -> new APIException("Error: Role not found"));

        user.setRoles(new HashSet<>(Set.of(userRole)));
        userRepository.save(user);
    }

    @Override
    public String currentUserName(Authentication authentication) {
        return authentication.getName();
    }

    @Override
    public UserInfoResponse getCurrentUserDetails(Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();

        return new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(), roles, userDetails.getEmail());
    }

//    @Override
//    public AuthenticationResult logoutUser() {
//        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
//        return new AuthenticationResult(null, cookie);
//    }

    @Override
    public SellerResponse getAllSellers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<User> allUsers = userRepository.findByRoleName(AppRole.ROLE_SELLER, pageable);
        List<SellerDTO> sellerDTOS = allUsers.getContent()
                .stream()
                .map(user -> {
                    SellerDTO sellerDTO = modelMapper.map(user, SellerDTO.class);
                    List<AddressDTO> addressDTOS = user.getAddresses()
                            .stream()
                            .map(address -> modelMapper.map(address, AddressDTO.class))
                            .toList();
                    sellerDTO.setAddresses(addressDTOS);

                    Set<String> roles = user.getRoles().stream()
                                    .map(role -> role.getRoleName().name())
                            .collect(Collectors.toSet());
                    sellerDTO.setRoles(roles);
                    return sellerDTO;
                })
                .toList();

        SellerResponse sellerResponse = new SellerResponse();
        sellerResponse.setContent(sellerDTOS);
        sellerResponse.setPageNumber(allUsers.getNumber());
        sellerResponse.setPageSize(allUsers.getSize());
        sellerResponse.setTotalElements(allUsers.getTotalElements());
        sellerResponse.setTotalPages(allUsers.getTotalPages());
        sellerResponse.setLastPage(allUsers.isLast());
        return sellerResponse;
    }

    @Override
    public void promoteUser(Long userId, PromoteRoleRequestDTO requestDTO) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        String normalizedRole = requestDTO.getRole().trim().toUpperCase();

        AppRole appRole;

        try {
            appRole = AppRole.valueOf(normalizedRole);
        } catch (IllegalArgumentException e) {
            throw new APIException("Invalid role: " + requestDTO.getRole());
        }

        if (appRole == AppRole.ROLE_USER) {
            throw new APIException("Users already have ROLE_USER");
        }

        Role role = roleRepository.findByRoleName(appRole)
                .orElseThrow(() -> new APIException("Role not found"));

        if (user.getRoles().contains(role)) {
            throw new APIException("User already has role: " + appRole);
        }

        user.getRoles().add(role);

        userRepository.save(user);
    }
}
