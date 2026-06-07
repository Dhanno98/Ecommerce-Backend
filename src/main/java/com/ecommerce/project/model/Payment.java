package com.ecommerce.project.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @SequenceGenerator(
            name = "payment_seq_generator",
            sequenceName = "payment_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_seq_generator")
    private Long paymentId;

    @OneToOne(mappedBy = "payment")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    private String pgPaymentId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus pgStatus;

    private String pgResponseMessage;
    private String pgName;

    public Payment(PaymentMethod paymentMethod, String pgPaymentId, PaymentStatus pgStatus, String pgResponseMessage, String pgName) {
        this.paymentMethod = paymentMethod;
        this.pgPaymentId = pgPaymentId;
        this.pgStatus = pgStatus;
        this.pgResponseMessage = pgResponseMessage;
        this.pgName = pgName;
    }

}
