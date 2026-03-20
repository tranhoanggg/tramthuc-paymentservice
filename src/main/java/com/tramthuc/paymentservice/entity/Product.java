package com.tramthuc.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

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

    @Column(nullable = false)
    private BigDecimal price; 
    
    @Column(nullable = false, columnDefinition = "int default 100")
    private Integer stock; 

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long version = 0L;
}