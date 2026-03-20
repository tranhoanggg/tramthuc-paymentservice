package com.tramthuc.paymentservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {
    
    @NotNull(message = "error.order.product_id_not_null")
    private Long productId;

    @NotNull(message = "error.order.quantity_not_null")
    @Min(value = 1, message = "error.order.quantity_min")
    private Integer quantity;
}