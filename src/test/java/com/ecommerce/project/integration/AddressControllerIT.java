package com.ecommerce.project.integration;

import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.OrderStatus;
import com.ecommerce.project.model.Payment;
import com.ecommerce.project.model.PaymentMethod;
import com.ecommerce.project.model.PaymentStatus;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.OrderRepository;
import com.ecommerce.project.repositories.PaymentRepository;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.services.UserDetailsImpl;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AddressControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    /// createAddress
    @Test
    void createAddressShouldSuccessfullyCreateAddress() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        AddressDTO addressDTO = createAddressDTO("123 Maple Street, Apt 4B", "Oakwood Commons", "New York",
                "NY", "USA", "627045");
        String json = objectMapper.writeValueAsString(addressDTO);

        mockMvc.perform(post("/api/addresses")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.addressId").isNumber())
                .andExpect(jsonPath("$.street").value(addressDTO.getStreet()))
                .andExpect(jsonPath("$.buildingName").value(addressDTO.getBuildingName()))
                .andExpect(jsonPath("$.city").value(addressDTO.getCity()))
                .andExpect(jsonPath("$.state").value(addressDTO.getState()))
                .andExpect(jsonPath("$.country").value(addressDTO.getCountry()))
                .andExpect(jsonPath("$.pincode").value(addressDTO.getPincode()));

        List<Address> addresses = addressRepository.findByUserId(savedUser.getUserId());
        assertEquals(1, addresses.size());
        Address addressInDB = addresses.getFirst();

        assertEquals(savedUser, addressInDB.getUser());
        assertEquals(addressDTO.getStreet(), addressInDB.getStreet());
        assertEquals(addressDTO.getBuildingName(), addressInDB.getBuildingName());
        assertEquals(addressDTO.getCity(), addressInDB.getCity());
        assertEquals(addressDTO.getState(), addressInDB.getState());
        assertEquals(addressDTO.getCountry(), addressInDB.getCountry());
        assertEquals(addressDTO.getPincode(), addressInDB.getPincode());
    }

    @Test
    void createAddressShouldReturnBadRequestIfAddressDtoIsInvalid() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        AddressDTO addressDTO = createAddressDTO("123 Maple Street, Apt 4B", "Oakwood Commons", "New York",
                "NY", "USA", "62704");
        String json = objectMapper.writeValueAsString(addressDTO);

        mockMvc.perform(post("/api/addresses")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pincode").value("Pincode name must be at least 6 characters"));
    }

    @Test
    void createAddressShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        AddressDTO addressDTO = createAddressDTO("123 Maple Street, Apt 4B", "Oakwood Commons", "New York",
                "NY", "USA", "627045");
        String json = objectMapper.writeValueAsString(addressDTO);

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// getAllAddresses()
    @Test
    void getAllAddressesShouldSuccessfullyReturnAllAddressesIfUserIsAdmin() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();
        Role roleAdmin = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        Address address1 = createAddress(savedUser, "123 Maple Street, Apt 4B", "Oakwood Commons",
                "New York", "NY", "USA", "627045");
        Address savedAddress1 = addressRepository.save(address1);

        Address address2 = createAddress(savedUser, "123 Orange Street, Apt 4B", "Greenwood Commons",
                "Los Angeles", "CA", "USA", "900012");
        Address savedAddress2 = addressRepository.save(address2);

        User admin = createUser("Test Admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        mockMvc.perform(get("/api/admin/addresses")
                        .with(user(userDetails))
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(2))

                .andExpect(jsonPath("$.content[0].addressId").value(savedAddress1.getAddressId()))
                .andExpect(jsonPath("$.content[0].street").value(savedAddress1.getStreet()))
                .andExpect(jsonPath("$.content[0].buildingName").value(savedAddress1.getBuildingName()))
                .andExpect(jsonPath("$.content[0].city").value(savedAddress1.getCity()))
                .andExpect(jsonPath("$.content[0].state").value(savedAddress1.getState()))
                .andExpect(jsonPath("$.content[0].country").value(savedAddress1.getCountry()))
                .andExpect(jsonPath("$.content[0].pincode").value(savedAddress1.getPincode()))

                .andExpect(jsonPath("$.content[1].addressId").value(savedAddress2.getAddressId()))
                .andExpect(jsonPath("$.content[1].street").value(savedAddress2.getStreet()))
                .andExpect(jsonPath("$.content[1].buildingName").value(savedAddress2.getBuildingName()))
                .andExpect(jsonPath("$.content[1].city").value(savedAddress2.getCity()))
                .andExpect(jsonPath("$.content[1].state").value(savedAddress2.getState()))
                .andExpect(jsonPath("$.content[1].country").value(savedAddress2.getCountry()))
                .andExpect(jsonPath("$.content[1].pincode").value(savedAddress2.getPincode()))

                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));

        assertEquals(2, addressRepository.count());
    }

    @Test
    void getAllAddressesShouldReturnEmptyPageIfNoAddressExists() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User admin = createUser("Test Admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        mockMvc.perform(get("/api/admin/addresses")
                        .with(user(userDetails))
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void getAllAddressesShouldReturnBadRequestIfPaginationParametersAreInvalid() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User admin = createUser("Test Admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        mockMvc.perform(get("/api/admin/addresses")
                        .with(user(userDetails))
                        .param("sortOrder", "ascending"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid sort order"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void getAllAddressesShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/admin/addresses")
                        .with(user(userDetails))
                        .param("sortOrder", "asc"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getAllAddressesShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/addresses")
                        .param("sortOrder", "asc"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// getAddressById()
    @Test
    void getAddressByIdShouldSuccessfullyReturnAddressById() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();
        Role roleAdmin = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        Address address1 = createAddress(savedUser, "123 Maple Street, Apt 4B", "Oakwood Commons",
                "New York", "NY", "USA", "627045");
        Address savedAddress1 = addressRepository.save(address1);

        Address address2 = createAddress(savedUser, "123 Orange Street, Apt 4B", "Greenwood Commons",
                "Los Angeles", "CA", "USA", "900012");
        addressRepository.save(address2);

        User admin = createUser("Test Admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        Long addressId = savedAddress1.getAddressId();

        mockMvc.perform(get("/api/admin/addresses/{addressId}", addressId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.addressId").value(addressId))
                .andExpect(jsonPath("$.street").value(savedAddress1.getStreet()))
                .andExpect(jsonPath("$.buildingName").value(savedAddress1.getBuildingName()))
                .andExpect(jsonPath("$.city").value(savedAddress1.getCity()))
                .andExpect(jsonPath("$.state").value(savedAddress1.getState()))
                .andExpect(jsonPath("$.country").value(savedAddress1.getCountry()))
                .andExpect(jsonPath("$.pincode").value(savedAddress1.getPincode()));

        assertEquals(2, addressRepository.count());
    }

    @Test
    void getAddressByIdShouldReturnNotFoundIfAddressDoesNotExist() throws Exception {
        Role roleAdmin = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User admin = createUser("Test Admin", "admin@gmail.com", "password");
        admin.getRoles().add(roleAdmin);
        User savedAdmin = userRepository.save(admin);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        Long addressId = Long.MAX_VALUE;

        mockMvc.perform(get("/api/admin/addresses/{addressId}", addressId)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Address not found with addressId: " + addressId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void getAddressByIdShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
        Role roleSeller = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller = createUser("Test Seller", "seller@gmail.com", "password");
        seller.getRoles().add(roleSeller);
        User savedSeller = userRepository.save(seller);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller);

        Long addressId = 1L;

        mockMvc.perform(get("/api/admin/addresses/{addressId}", addressId)
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getAddressByIdShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Long addressId = 1L;

        mockMvc.perform(get("/api/admin/addresses/{addressId}", addressId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// getUserAddresses()
    @Test
    void getUserAddressesShouldSuccessfullyReturnAddressesOfTheLoggedInUser() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Address address1 = createAddress(savedUser, "123 Maple Street, Apt 4B", "Oakwood Commons",
                "New York", "NY", "USA", "627045");
        Address savedAddress1 = addressRepository.save(address1);

        Address address2 = createAddress(savedUser, "123 Orange Street, Apt 4B", "Greenwood Commons",
                "Los Angeles", "CA", "USA", "900012");
        Address savedAddress2 = addressRepository.save(address2);

        mockMvc.perform(get("/api/users/addresses")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                .andExpect(jsonPath("$.length()").value(2))

                .andExpect(jsonPath("$[0].addressId").value(savedAddress1.getAddressId()))
                .andExpect(jsonPath("$[0].street").value(savedAddress1.getStreet()))
                .andExpect(jsonPath("$[0].buildingName").value(savedAddress1.getBuildingName()))
                .andExpect(jsonPath("$[0].city").value(savedAddress1.getCity()))
                .andExpect(jsonPath("$[0].state").value(savedAddress1.getState()))
                .andExpect(jsonPath("$[0].country").value(savedAddress1.getCountry()))
                .andExpect(jsonPath("$[0].pincode").value(savedAddress1.getPincode()))

                .andExpect(jsonPath("$[1].addressId").value(savedAddress2.getAddressId()))
                .andExpect(jsonPath("$[1].street").value(savedAddress2.getStreet()))
                .andExpect(jsonPath("$[1].buildingName").value(savedAddress2.getBuildingName()))
                .andExpect(jsonPath("$[1].city").value(savedAddress2.getCity()))
                .andExpect(jsonPath("$[1].state").value(savedAddress2.getState()))
                .andExpect(jsonPath("$[1].country").value(savedAddress2.getCountry()))
                .andExpect(jsonPath("$[1].pincode").value(savedAddress2.getPincode()));
    }

    @Test
    void getUserAddressesShouldReturnEmptyIfTheLoggedInUserHasNoAddresses() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/users/addresses")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getUserAddressesShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users/addresses"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// updateAddress()
    @Test
    void updateAddressShouldSuccessfullyUpdateAddressOfTheLoggedInUser() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Address address = createAddress(savedUser, "123 Maple Street, Apt 4B", "Oakwood Commons",
                "New York", "NY", "USA", "627045");
        Address savedAddress = addressRepository.save(address);

        AddressDTO addressDTO = createAddressDTO("123 Orange Street, Apt 4B", "Greenwood Commons",
                "Los Angeles", "CA", "USA", "900012");
        String json = objectMapper.writeValueAsString(addressDTO);

        Long addressId = savedAddress.getAddressId();

        mockMvc.perform(put("/api/addresses/{addressId}", addressId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.addressId").value(addressId))
                .andExpect(jsonPath("$.street").value(addressDTO.getStreet()))
                .andExpect(jsonPath("$.buildingName").value(addressDTO.getBuildingName()))
                .andExpect(jsonPath("$.city").value(addressDTO.getCity()))
                .andExpect(jsonPath("$.state").value(addressDTO.getState()))
                .andExpect(jsonPath("$.country").value(addressDTO.getCountry()))
                .andExpect(jsonPath("$.pincode").value(addressDTO.getPincode()));

        Address addressInDB = addressRepository.findById(addressId).orElseThrow();
        assertEquals(addressId, addressInDB.getAddressId());
        assertEquals(savedUser, addressInDB.getUser());
        assertEquals(addressDTO.getStreet(), addressInDB.getStreet());
        assertEquals(addressDTO.getBuildingName(), addressInDB.getBuildingName());
        assertEquals(addressDTO.getCity(), addressInDB.getCity());
        assertEquals(addressDTO.getState(), addressInDB.getState());
        assertEquals(addressDTO.getCountry(), addressInDB.getCountry());
        assertEquals(addressDTO.getPincode(), addressInDB.getPincode());
    }

    @Test
    void updateAddressShouldReturnNotFoundIfAddressDoesNotExist() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser1 = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser1);

        AddressDTO addressDTO = createAddressDTO("123 Orange Street, Apt 4B", "Greenwood Commons",
                "Los Angeles", "CA", "USA", "900012");
        String json = objectMapper.writeValueAsString(addressDTO);

        Long addressId = Long.MAX_VALUE;

        mockMvc.perform(put("/api/addresses/{addressId}", addressId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Address not found with addressId: " + addressId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateAddressShouldReturnNotFoundIfAddressDoesNotBelongToTheLoggedInUser() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user1 = createUser("Test User 1", "user1@gmail.com", "password");
        user1.getRoles().add(roleUser);
        User savedUser1 = userRepository.save(user1);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser1);

        User user2 = createUser("Test User 2", "user2@gmail.com", "password");
        user2.getRoles().add(roleUser);
        User savedUser2 = userRepository.save(user2);

        Address address = createAddress(savedUser2, "123 Maple Street, Apt 4B", "Oakwood Commons",
                "New York", "NY", "USA", "627045");
        Address savedAddress = addressRepository.save(address);

        AddressDTO addressDTO = createAddressDTO("123 Orange Street, Apt 4B", "Greenwood Commons",
                "Los Angeles", "CA", "USA", "900012");
        String json = objectMapper.writeValueAsString(addressDTO);

        Long addressId = savedAddress.getAddressId();

        mockMvc.perform(put("/api/addresses/{addressId}", addressId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Address not found with addressId: " + addressId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateAddressShouldReturnBadRequestIfAddressDtoIsInvalid() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        AddressDTO addressDTO = createAddressDTO("123 Orange Street, Apt 4B", "Greenwood Commons",
                "Los Angeles", "CA", "USA", "90001");
        String json = objectMapper.writeValueAsString(addressDTO);

        Long addressId = 1L;

        mockMvc.perform(put("/api/addresses/{addressId}", addressId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pincode").value("Pincode name must be at least 6 characters"));
    }

    @Test
    void updateAddressShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        userRepository.save(user);

        AddressDTO addressDTO = createAddressDTO("123 Orange Street, Apt 4B", "Greenwood Commons",
                "Los Angeles", "CA", "USA", "900012");
        String json = objectMapper.writeValueAsString(addressDTO);

        Long addressId = 1L;

        mockMvc.perform(put("/api/addresses/{addressId}", addressId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// deleteAddress()
    @Test
    void deleteAddressShouldSuccessfullyDeleteAddressOwnedByTheUser() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Address address1 = createAddress(savedUser, "123 Maple Street, Apt 4B", "Oakwood Commons",
                "New York", "NY", "USA", "627045");
        Address savedAddress1 = addressRepository.save(address1);

        Address address2 = createAddress(savedUser, "123 Orange Street, Apt 4B", "Greenwood Commons",
                "Los Angeles", "CA", "USA", "900012");
        Address savedAddress2 = addressRepository.save(address2);

        assertEquals(2, addressRepository.count());
        assertTrue(addressRepository.existsById(savedAddress1.getAddressId()));
        assertTrue(addressRepository.existsById(savedAddress2.getAddressId()));

        Long addressId = savedAddress1.getAddressId();

        mockMvc.perform(delete("/api/addresses/{addressId}", addressId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string("Address deleted successfully with addressId: " + addressId));

        assertEquals(1, addressRepository.count());
        assertFalse(addressRepository.existsById(addressId));
        assertTrue(addressRepository.existsById(savedAddress2.getAddressId()));
    }

    @Test
    void deleteAddressShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Long addressId = 1L;

        mockMvc.perform(delete("/api/addresses/{addressId}", addressId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void deleteAddressShouldReturnNotFoundIfAddressDoesNotExist() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long addressId = Long.MAX_VALUE;

        mockMvc.perform(delete("/api/addresses/{addressId}", addressId)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Address not found with addressId: " + addressId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void deleteAddressShouldReturnNotFoundIfAddressDoesNotBelongToTheLoggedInUser() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user1 = createUser("Test User 1", "user1@gmail.com", "password");
        user1.getRoles().add(roleUser);
        User savedUser1 = userRepository.save(user1);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser1);

        User user2 = createUser("Test User 2", "user2@gmail.com", "password");
        user2.getRoles().add(roleUser);
        User savedUser2 = userRepository.save(user2);

        Address address = createAddress(savedUser2, "123 Maple Street, Apt 4B", "Oakwood Commons",
                "New York", "NY", "USA", "627045");
        Address savedAddress = addressRepository.save(address);

        Long addressId = savedAddress.getAddressId();

        mockMvc.perform(delete("/api/addresses/{addressId}", addressId)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Address not found with addressId: " + addressId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void deleteAddressShouldReturnBadRequestIfAddressToBeDeletedIsAssociatedWithAnOrder() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Address address = createAddress(savedUser, "123 Maple Street, Apt 4B", "Oakwood Commons",
                "New York", "NY", "USA", "627045");
        Address savedAddress = addressRepository.save(address);

        Payment payment = createPayment(PaymentMethod.CARD);
        Payment savedPayment = paymentRepository.save(payment);

        Order order = createOrder(savedUser, savedAddress, savedPayment);
        orderRepository.save(order);

        assertEquals(1, addressRepository.count());

        Long addressId = savedAddress.getAddressId();

        mockMvc.perform(delete("/api/addresses/{addressId}", addressId)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Cannot delete address because it is associated with existing orders"))
                .andExpect(jsonPath("$.status").value(false));

        assertEquals(1, addressRepository.count());
        assertTrue(addressRepository.existsById(addressId));
    }

    /// HELPERS
    private User createUser(String userName, String email, String password) {
        User user = new User();
        user.setUserName(userName);
        user.setEmail(email);
        user.setPassword(password);
        user.setRoles(new HashSet<>());
        user.setAddresses(new ArrayList<>());
        return user;
    }

    private Address createAddress(User user, String street, String buildingName, String city, String state, String country, String pincode) {
        Address address = new Address();
        address.setStreet(street);
        address.setBuildingName(buildingName);
        address.setCity(city);
        address.setState(state);
        address.setCountry(country);
        address.setPincode(pincode);
        address.setUser(user);
        return address;
    }

    private Payment createPayment(PaymentMethod paymentMethod) {
        return new Payment(
                paymentMethod,
                "MOCK",
                PaymentStatus.SUCCESS,
                "Payment Successful",
                "Stripe"
        );
    }

    private Order createOrder(User user, Address address, Payment payment) {
        Order order = new Order();
        order.setEmail(user.getEmail());
        order.setPayment(payment);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setAddress(address);
        order.setOrderDate(LocalDateTime.now());
        return order;
    }

    private AddressDTO createAddressDTO(String street, String buildingName, String city, String state, String country, String pincode) {
        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setStreet(street);
        addressDTO.setBuildingName(buildingName);
        addressDTO.setCity(city);
        addressDTO.setState(state);
        addressDTO.setCountry(country);
        addressDTO.setPincode(pincode);
        return addressDTO;
    }
}
