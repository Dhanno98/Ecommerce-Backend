package com.ecommerce.project.service;

import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.payload.AddressResponse;

import java.util.List;

public interface AddressService {
    AddressDTO createAddress(AddressDTO addressDTO);

    AddressResponse getAllAddresses(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    AddressDTO getAddressById(Long addressId);

    List<AddressDTO> getUserAddresses();

    AddressDTO updateAddress(Long addressId, AddressDTO addressDTO);

    String deleteAddress(Long addressId);
}
