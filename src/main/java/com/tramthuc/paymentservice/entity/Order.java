package com.tramthuc.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; 

    @Column(nullable = false)
    private String userEmail; 

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerPhone;

    @Column(nullable = false)
    private String shippingAddress;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    // 1. TRẠNG THÁI THANH TOÁN (UNPAID, PAID, REFUND_REQUESTED, REFUNDED)
    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    // 2. TRẠNG THÁI GIAO HÀNG (CREATED, SHIPPING, COMPLETED, CANCELLED)
    @Column(name = "delivery_status", nullable = false)
    private String deliveryStatus;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(length = 500)
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        if (this.deliveryStatus == null) {
            this.deliveryStatus = "CREATED"; 
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = "UNPAID"; 
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}