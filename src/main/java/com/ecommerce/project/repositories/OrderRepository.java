package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    Double getTotalRevenue();

    @EntityGraph(attributePaths = {
            "orderItems", "orderItems.product", "payment"
    })
    Page<Order> findAll(Pageable pageDetails);

    @EntityGraph(attributePaths = {
            "orderItems", "orderItems.product", "payment"
    })
    Optional<Order> findOrderWithDetailsByOrderId(Long orderId);

    @EntityGraph(attributePaths = {
            "orderItems", "orderItems.product"
    })
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.product p WHERE p.user.userId = :sellerId")
    Page<Order> findOrderBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.address.addressId = ?1")
    boolean existsByAddressId(Long addressId);
}
