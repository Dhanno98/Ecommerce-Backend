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
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AuthUtil authUtil;

    private final ModelMapper modelMapper;

    private final AddressRepository addressRepository;

    private final PaginationValidator paginationValidator;

    private final OrderRepository orderRepository;

    private final static List<String> ALLOWED_SORT_FIELDS = List.of("addressId", "street", "buildingName", "city", "state", "country", "pincode");

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO) {
        User user = authUtil.loggedInUser();

        Address address = modelMapper.map(addressDTO, Address.class);

        user.getAddresses().add(address);
        address.setUser(user);

        Address savedAddress = addressRepository.save(address);
        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public AddressResponse getAllAddresses(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Address> pageAddresses = addressRepository.findAll(pageable);
        List<Address> addresses = pageAddresses.getContent();

        List<AddressDTO> addressDTOS = addresses.stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();

        AddressResponse addressResponse = new AddressResponse();
        addressResponse.setContent(addressDTOS);
        addressResponse.setPageNumber(pageAddresses.getNumber());
        addressResponse.setPageSize(pageAddresses.getSize());
        addressResponse.setTotalElements(pageAddresses.getTotalElements());
        addressResponse.setTotalPages(pageAddresses.getTotalPages());
        addressResponse.setLastPage(pageAddresses.isLast());
        return addressResponse;
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        return modelMapper.map(address, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getUserAddresses() {
        Long userId = authUtil.loggedInUserId();

        List<Address> addresses = addressRepository.findByUserId(userId);

        return addresses.stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();
    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Long userId = authUtil.loggedInUserId();
        Address addressFromDB = addressRepository.findByAddressIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        addressFromDB.setStreet(addressDTO.getStreet());
        addressFromDB.setBuildingName(addressDTO.getBuildingName());
        addressFromDB.setCity(addressDTO.getCity());
        addressFromDB.setState(addressDTO.getState());
        addressFromDB.setCountry(addressDTO.getCountry());
        addressFromDB.setPincode(addressDTO.getPincode());

        Address updatedAddress = addressRepository.save(addressFromDB);
        return modelMapper.map(updatedAddress, AddressDTO.class);
    }

    @Override
    public String deleteAddress(Long addressId) {
        Long userId = authUtil.loggedInUserId();
        Address addressFromDB = addressRepository.findByAddressIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        if (orderRepository.existsByAddressId(addressId)) {
            throw new APIException("Cannot delete address because it is associated with existing orders");
        }

        addressRepository.delete(addressFromDB);
        return "Address deleted successfully with addressId: " + addressId;
    }
}
