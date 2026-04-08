package com.tramthuc.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products") 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @Column(nullable = false)
    private String name; 
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price; 
    
    @Column(name = "original_price")
    private Integer originalPrice;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;
    
    @Column(length = 100)
    private String category;
    
    @Column(name = "is_best_seller")
    private Boolean isBestSeller;
    
    @Column(name = "sold_count")
    private Integer soldCount;
    
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false, columnDefinition = "int default 100")
    private Integer stock; 

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long version = 0L;
}