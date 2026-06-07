package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    @Query("SELECT a FROM Address a WHERE a.addressId = ?1 AND a.user.email = ?2")
    Optional<Address> findByIdAndUserEmailId(Long addressId, String emailId);
}
