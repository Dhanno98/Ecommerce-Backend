package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.payload.AddressResponse;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.OrderRepository;
import com.ecommerce.project.util.AuthUtil;
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
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
public class AddressServiceImplTest {

    @InjectMocks
    AddressServiceImpl addressService;

    @Mock
    AuthUtil authUtil;

    @Mock
    ModelMapper modelMapper;

    @Mock
    AddressRepository addressRepository;

    @Mock
    PaginationValidator paginationValidator;

    @Mock
    OrderRepository orderRepository;

    /// createAddress()
    @Test
    void createAddressShouldSuccessfullyCreateAddress() {
        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setStreet("123 Maple Street, Apt 4B");
        addressDTO.setBuildingName("Oakwood Commons");
        addressDTO.setCity("New York");
        addressDTO.setState("NY");
        addressDTO.setCountry("USA");
        addressDTO.setPincode("62704");

        User user = createUser(1L);
        user.setAddresses(new ArrayList<>());

        Address address = createAddress();

        Address savedAddress = createAddress();
        savedAddress.setAddressId(1L);
        savedAddress.setUser(user);

        AddressDTO savedAddressDTO = createAddressDTO(savedAddress);

        when(authUtil.loggedInUser())
                .thenReturn(user);

        when(modelMapper.map(addressDTO, Address.class))
                .thenReturn(address);

        when(addressRepository.save(address))
                .thenReturn(savedAddress);

        when(modelMapper.map(savedAddress, AddressDTO.class))
                .thenReturn(savedAddressDTO);

        AddressDTO result = addressService.createAddress(addressDTO);

        assertNotNull(result);
        assertEquals(addressDTO.getStreet(), result.getStreet());
        assertEquals(addressDTO.getBuildingName(), result.getBuildingName());
        assertEquals(addressDTO.getCity(), result.getCity());
        assertEquals(addressDTO.getState(), result.getState());
        assertEquals(addressDTO.getCountry(), result.getCountry());
        assertEquals(addressDTO.getPincode(), result.getPincode());

        assertEquals(savedAddressDTO.getAddressId(), result.getAddressId());

        assertEquals(user, address.getUser());

        assertEquals(1, user.getAddresses().size());
        assertTrue(user.getAddresses().contains(address));

        verify(authUtil).loggedInUser();
        verify(modelMapper).map(addressDTO, Address.class);
        verify(addressRepository).save(address);
        verify(modelMapper).map(savedAddress, AddressDTO.class);
    }

    /// getAllAddresses()
    @Test
    void getAllAddressesShouldReturnAllAddresses() {
        User user = createUser(1L);

        Address address = createAddress();
        address.setAddressId(1L);
        address.setUser(user);

        List<Address> addresses = List.of(address);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Address> addressPage = new PageImpl<>(addresses, pageable, addresses.size());

        AddressDTO addressDTO = createAddressDTO(address);

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(addressRepository.findAll(any(Pageable.class)))
                .thenReturn(addressPage);

        when(modelMapper.map(address, AddressDTO.class))
                .thenReturn(addressDTO);

        AddressResponse result = addressService.getAllAddresses(0, 10, "addressId", "asc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        AddressDTO returnedAddress = result.getContent().getFirst();
        assertEquals(address.getAddressId(), returnedAddress.getAddressId());

        verify(paginationValidator).validate(eq(0), eq(10), eq("addressId"), eq("asc"), anyList());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(addressRepository).findAll(pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();

        assertEquals(0, captured.getPageNumber());
        assertEquals(10, captured.getPageSize());

        Sort.Order sortOrder = captured.getSort().iterator().next();
        assertEquals("addressId", sortOrder.getProperty());
        assertEquals(Sort.Direction.ASC, sortOrder.getDirection());

        verify(modelMapper).map(address, AddressDTO.class);
    }

    @Test
    void getAllAddressesShouldReturnEmptyResponseWhenNoAddressesExist() {
        Page<Address> emptyPage = Page.empty();

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(addressRepository.findAll(any(Pageable.class)))
                .thenReturn(emptyPage);

        AddressResponse result = addressService.getAllAddresses(0, 10, "addressId", "asc");

        assertTrue(result.getContent().isEmpty());
        assertEquals(0L, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("addressId"), eq("asc"), anyList());
        verify(addressRepository).findAll(any(Pageable.class));
        verify(modelMapper, never()).map(any(Address.class), eq(AddressDTO.class));
    }

    /// getAddressById()
    @Test
    void getAddressByIdShouldReturnAddressById() {
        User user = createUser(1L);

        Address address = createAddress();
        address.setAddressId(1L);
        address.setUser(user);

        AddressDTO addressDTO = createAddressDTO(address);

        when(addressRepository.findById(1L))
                .thenReturn(Optional.of(address));

        when(modelMapper.map(address, AddressDTO.class))
                .thenReturn(addressDTO);

        AddressDTO result = addressService.getAddressById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getAddressId());

        verify(addressRepository).findById(1L);
        verify(modelMapper).map(address, AddressDTO.class);
    }

    @Test
    void getAddressByIdShouldThrowResourceNotFoundExceptionIfNoAddressHasThatID() {
        when(addressRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> addressService.getAddressById(1L)
        );

        assertEquals("Address not found with addressId: 1", exception.getMessage());

        verify(addressRepository).findById(1L);
        verifyNoInteractions(modelMapper);
    }

    /// getUserAddresses()
    @Test
    void getUserAddressesShouldReturnAllAddressesForThatUser() {
        User user = createUser(1L);

        Address address1 = createAddress("123 Maple Street, Apt 4B", "Oakwood Commons", "New York", "NY", "USA", "62704");
        address1.setAddressId(1L);
        address1.setUser(user);

        Address address2 = createAddress("123 Orange Street, Apt 4B", "Greenwood Commons", "Los Angeles", "CA", "USA", "90001");
        address2.setAddressId(2L);
        address2.setUser(user);

        AddressDTO addressDTO1 = createAddressDTO(address1);
        addressDTO1.setAddressId(1L);

        AddressDTO addressDTO2 = createAddressDTO(address2);
        addressDTO2.setAddressId(2L);

        List<Address> addresses = List.of(address1, address2);

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(addressRepository.findByUserId(user.getUserId()))
                .thenReturn(addresses);

        when(modelMapper.map(address1, AddressDTO.class))
                .thenReturn(addressDTO1);

        when(modelMapper.map(address2, AddressDTO.class))
                .thenReturn(addressDTO2);

        List<AddressDTO> result = addressService.getUserAddresses();

        assertNotNull(result);
        assertEquals(2, result.size());

        AddressDTO returnedAddress1 = result.get(0);
        assertEquals(address1.getAddressId(), returnedAddress1.getAddressId());

        AddressDTO returnedAddress2 = result.get(1);
        assertEquals(address2.getAddressId(), returnedAddress2.getAddressId());

        verify(authUtil).loggedInUserId();
        verify(addressRepository).findByUserId(user.getUserId());
        verify(modelMapper).map(address1, AddressDTO.class);
        verify(modelMapper).map(address2, AddressDTO.class);
    }

    @Test
    void getUserAddressesShouldReturnEmptyResponseIfUserHasNoAddress() {
        User user = createUser(1L);

        List<Address> addresses = new ArrayList<>();

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(addressRepository.findByUserId(user.getUserId()))
                .thenReturn(addresses);

        List<AddressDTO> result = addressService.getUserAddresses();

        assertNotNull(result);
        assertEquals(0, result.size());

        verify(authUtil).loggedInUserId();
        verify(addressRepository).findByUserId(user.getUserId());
        verifyNoInteractions(modelMapper);
    }

    /// updateAddress
    @Test
    void updateAddressShouldSuccessfullyUpdateAddress() {
        User user = createUser(1L);

        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setStreet("123 Orange Street, Apt 4B");
        addressDTO.setBuildingName("Greenwood Commons");
        addressDTO.setCity("Los Angeles");
        addressDTO.setState("CA");
        addressDTO.setCountry("USA");
        addressDTO.setPincode("90001");

        Address addressFromDB = createAddress("123 Maple Street, Apt 4B", "Oakwood Commons", "New York", "NY", "USA", "62704");
        addressFromDB.setAddressId(1L);
        addressFromDB.setUser(user);

        Address updatedAddress = createAddress(addressDTO.getStreet(), addressDTO.getBuildingName(), addressDTO.getCity(), addressDTO.getState(), addressDTO.getCountry(), addressDTO.getPincode());
        updatedAddress.setAddressId(addressFromDB.getAddressId());
        updatedAddress.setUser(user);

        AddressDTO updatedAddressDTO = createAddressDTO(updatedAddress);
        updatedAddressDTO.setAddressId(updatedAddress.getAddressId());

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(addressRepository.findByAddressIdAndUserId(1L, user.getUserId()))
                .thenReturn(Optional.of(addressFromDB));

        when(addressRepository.save(addressFromDB))
                .thenReturn(updatedAddress);

        when(modelMapper.map(updatedAddress, AddressDTO.class))
                .thenReturn(updatedAddressDTO);

        AddressDTO result = addressService.updateAddress(1L, addressDTO);

        assertNotNull(result);
        assertEquals(addressDTO.getStreet(), result.getStreet());
        assertEquals(addressDTO.getBuildingName(), result.getBuildingName());
        assertEquals(addressDTO.getCity(), result.getCity());
        assertEquals(addressDTO.getState(), result.getState());
        assertEquals(addressDTO.getCountry(), result.getCountry());
        assertEquals(addressDTO.getPincode(), result.getPincode());

        assertEquals(1L, result.getAddressId());

        assertEquals(addressDTO.getStreet(), addressFromDB.getStreet());
        assertEquals(addressDTO.getBuildingName(), addressFromDB.getBuildingName());
        assertEquals(addressDTO.getCity(), addressFromDB.getCity());
        assertEquals(addressDTO.getState(), addressFromDB.getState());
        assertEquals(addressDTO.getCountry(), addressFromDB.getCountry());
        assertEquals(addressDTO.getPincode(), addressFromDB.getPincode());

        verify(authUtil).loggedInUserId();
        verify(addressRepository).findByAddressIdAndUserId(1L, user.getUserId());
        verify(addressRepository).save(addressFromDB);
        verify(modelMapper).map(updatedAddress, AddressDTO.class);
    }

    @Test
    void updateAddressShouldThrowResourceNotFoundExceptionIfAddressDoesNotExist() {
        User user = createUser(1L);

        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setStreet("123 Orange Street, Apt 4B");
        addressDTO.setBuildingName("Greenwood Commons");
        addressDTO.setCity("Los Angeles");
        addressDTO.setState("CA");
        addressDTO.setCountry("USA");
        addressDTO.setPincode("90001");

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(addressRepository.findByAddressIdAndUserId(1L, user.getUserId()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> addressService.updateAddress(1L, addressDTO)
        );

        assertEquals("Address not found with addressId: 1", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(addressRepository).findByAddressIdAndUserId(1L, user.getUserId());
        verify(addressRepository, never()).save(any(Address.class));
        verifyNoInteractions(modelMapper);
    }

    /// deleteAddress()
    @Test
    void deleteAddressShouldSuccessfullyDeleteAddress() {
        User user = createUser(1L);

        Address addressFromDB = createAddress();
        addressFromDB.setAddressId(1L);
        addressFromDB.setUser(user);

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(addressRepository.findByAddressIdAndUserId(1L, user.getUserId()))
                .thenReturn(Optional.of(addressFromDB));

        when(orderRepository.existsByAddressId(addressFromDB.getAddressId()))
                .thenReturn(false);

        doNothing().when(addressRepository).delete(addressFromDB);

        String result = addressService.deleteAddress(1L);

        assertNotNull(result);
        assertEquals("Address deleted successfully with addressId: " + addressFromDB.getAddressId(), result);

        verify(authUtil).loggedInUserId();
        verify(addressRepository).findByAddressIdAndUserId(1L, user.getUserId());
        verify(orderRepository).existsByAddressId(addressFromDB.getAddressId());
        verify(addressRepository).delete(addressFromDB);
    }

    @Test
    void deleteAddressShouldThrowResourceNotFoundExceptionIfAddressDoesNotExist() {
        User user = createUser(1L);

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(addressRepository.findByAddressIdAndUserId(1L, user.getUserId()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> addressService.deleteAddress(1L)
        );

        assertEquals("Address not found with addressId: 1", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(addressRepository).findByAddressIdAndUserId(1L, user.getUserId());
        verifyNoInteractions(orderRepository);
        verify(addressRepository, never()).delete(any(Address.class));
    }

    @Test
    void deleteAddressShouldThrowApiExceptionWhenAddressIsLinkedWithAnOrder() {
        User user = createUser(1L);

        Address addressFromDB = createAddress();
        addressFromDB.setAddressId(1L);
        addressFromDB.setUser(user);

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(addressRepository.findByAddressIdAndUserId(1L, user.getUserId()))
                .thenReturn(Optional.of(addressFromDB));

        when(orderRepository.existsByAddressId(addressFromDB.getAddressId()))
                .thenReturn(true);

        APIException exception = assertThrows(
                APIException.class,
                () -> addressService.deleteAddress(1L)
        );

        assertEquals("Cannot delete address because it is associated with existing orders", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(addressRepository).findByAddressIdAndUserId(1L, user.getUserId());
        verify(orderRepository).existsByAddressId(addressFromDB.getAddressId());
        verify(addressRepository, never()).delete(any(Address.class));
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

    private Address createAddress() {
        Address address = new Address();

        address.setStreet("123 Maple Street, Apt 4B");
        address.setBuildingName("Oakwood Commons");
        address.setCity("New York");
        address.setState("NY");
        address.setCountry("USA");
        address.setPincode("62704");

        return address;
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
