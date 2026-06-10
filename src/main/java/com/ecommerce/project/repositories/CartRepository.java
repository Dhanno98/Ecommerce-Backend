package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = {
            "cartItems", "cartItems.product"
    })
    @Query("SELECT c FROM Cart c WHERE c.user.email = ?1")
    Cart findCartByEmail(String email);

    @EntityGraph(attributePaths = {
            "cartItems", "cartItems.product"
    })
    @Query("SELECT c FROM Cart c WHERE c.user.userId = ?1")
    Cart findCartByUserId(Long userId);

    @EntityGraph(attributePaths = {
            "cartItems", "cartItems.product"
    })
    List<Cart> findAll();
}
