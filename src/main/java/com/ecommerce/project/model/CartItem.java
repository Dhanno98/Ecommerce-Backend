package com.ecommerce.project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cart_items",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"cart_id", "product_id"})
        }
)
public class CartItem {

    @Id
    @SequenceGenerator(
            name = "cart_item_seq_generator",
            sequenceName = "cart_item_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cart_item_seq_generator")
    private Long cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double discount;

    @Column(nullable = false)
    private Double productPrice;
}
