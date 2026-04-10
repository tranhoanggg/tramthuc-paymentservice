package com.tramthuc.paymentservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    
    @NotBlank(message = "error.order.customer_name_not_null")
    private String customerName;

    @NotBlank(message = "error.order.customer_phone_not_null")
    private String customerPhone;

    @NotBlank(message = "error.order.shipping_address_not_null")
    private String shippingAddress;

    @NotBlank(message = "error.order.payment_method_not_null")
    private String paymentMethod;

    private String note;

    @NotEmpty(message = "error.order.cart_empty")
    @Valid
    private List<OrderItemRequest> items; 
}