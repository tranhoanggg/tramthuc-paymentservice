package com.tramthuc.paymentservice.dto.response;

import com.tramthuc.paymentservice.entity.Order;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {
    private Order order;
    private String paymentUrl;
}