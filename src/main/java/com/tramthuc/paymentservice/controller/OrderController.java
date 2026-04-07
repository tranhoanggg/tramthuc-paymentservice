package com.tramthuc.paymentservice.controller;

import com.tramthuc.paymentservice.dto.request.OrderRequest;
import com.tramthuc.paymentservice.dto.response.ApiResponse;
import com.tramthuc.paymentservice.dto.response.OrderResponse;
import com.tramthuc.paymentservice.entity.Order;
import com.tramthuc.paymentservice.service.OrderService;
import com.tramthuc.paymentservice.service.VNPayService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final VNPayService vnPayService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Email", defaultValue = "khachhang@gmail.com") String userEmail,
            HttpServletRequest httpRequest) { 
        
        // Truyền thêm httpRequest xuống Service
        OrderResponse responseData = orderService.createOrder(request, userId, userEmail, httpRequest);
        
        return ResponseEntity.ok(ApiResponse.success("Tạo đơn hàng thành công!", responseData));
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<String>> vnpayReturn(HttpServletRequest request) {
        // 1. Xác thực chữ ký
        boolean isValidSignature = vnPayService.verifyPayment(request);
        if (!isValidSignature) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Chữ ký không hợp lệ hoặc dữ liệu bị giả mạo!"));
        }
        
        // 2. Lấy mã đơn hàng và mã phản hồi từ VNPay
        String orderIdStr = request.getParameter("vnp_TxnRef");
        String responseCode = request.getParameter("vnp_ResponseCode");

        // 3. Xử lý logic
        Long orderId = Long.parseLong(orderIdStr);
        String frontendSuccessUrl = "http://localhost:3000/orders/success";
        
        if ("00".equals(responseCode)) {
            orderService.updatePaymentStatus(orderId, "PAID");
            
            return ResponseEntity.status(302)
                .location(java.net.URI.create(frontendSuccessUrl + "?status=success&orderId=" + orderIdStr))
                .build();
        } else {
            orderService.updatePaymentStatus(orderId, "FAILED");
            
            return ResponseEntity.status(302)
                .location(java.net.URI.create(frontendSuccessUrl + "?status=error&message=PaymentFailed"))
                .build();
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<Order>>> getMyOrders(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        
        List<Order> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công!", orders));
    }

    @PutMapping("/{orderId}/complete-cod")
    public ResponseEntity<ApiResponse<Order>> completeCodOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        
        Order order = orderService.completeCodOrder(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thanh toán COD thành công!", order));
    }
}