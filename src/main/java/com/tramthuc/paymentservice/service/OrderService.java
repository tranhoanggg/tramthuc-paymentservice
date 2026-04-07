package com.tramthuc.paymentservice.service;

import com.tramthuc.paymentservice.dto.request.OrderRequest;
import com.tramthuc.paymentservice.dto.response.OrderResponse;
import com.tramthuc.paymentservice.dto.request.OrderItemRequest;
import com.tramthuc.paymentservice.entity.Order;
import com.tramthuc.paymentservice.entity.OrderItem;
import com.tramthuc.paymentservice.entity.Product;
import com.tramthuc.paymentservice.repository.OrderRepository;
import com.tramthuc.paymentservice.repository.ProductRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final VNPayService vnPayService;

    @Transactional
    public OrderResponse createOrder(OrderRequest request, Long userId, String userEmail, HttpServletRequest httpRequest) {
        
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("error.order.cart_empty"); 
        }

        String method = request.getPaymentMethod().toUpperCase();
        if (!method.equals("COD") && !method.equals("VNPAY")) {
            throw new RuntimeException("error.order.invalid_payment_method");
        }
        
        Order order = Order.builder()
                .userId(userId)
                .userEmail(userEmail)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .shippingAddress(request.getShippingAddress())
                .paymentMethod(method)
                // CẬP NHẬT: Thay thế "status" bằng 2 trường riêng biệt
                .paymentStatus("UNPAID") 
                .deliveryStatus("CREATED") 
                .totalAmount(BigDecimal.ZERO) 
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 2. Xử lý từng món hàng
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("error.product.not_found"));

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new RuntimeException("error.product.out_of_stock"); 
            }

            // Trừ tồn kho
            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);

            // Tính tiền
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            // Tạo OrderItem
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .price(product.getPrice())
                    .build();

            order.getItems().add(orderItem);
        }

        // 3. Chốt tổng tiền và Lưu
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // --- PHÂN LUỒNG TRẢ VỀ ---
        String paymentUrl = null;
        if (method.equals("VNPAY")) {
            long amountInVND = totalAmount.longValue();
            String ipAddr = httpRequest.getRemoteAddr(); 
            
            paymentUrl = vnPayService.createOrderPaymentUrl(savedOrder.getId(), amountInVND, ipAddr);
        }

        return OrderResponse.builder()
                .order(savedOrder)
                .paymentUrl(paymentUrl)
                .build();
    }

    // CẬP NHẬT: Tách hàm update trạng thái thành 2 hàm độc lập cho Thanh toán và Giao hàng
    @Transactional
    public void updatePaymentStatus(Long orderId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("error.order.not_found"));
        order.setPaymentStatus(paymentStatus);
        orderRepository.save(order);
    }

    @Transactional
    public void updateDeliveryStatus(Long orderId, String deliveryStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("error.order.not_found"));
        order.setDeliveryStatus(deliveryStatus);
        orderRepository.save(order);
    }

    // 1. Hàm lấy danh sách đơn hàng của một User
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 2. Hàm xác nhận thanh toán cho đơn COD
    @Transactional
    public Order completeCodOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("error.order.not_found"));

        // Bảo mật: Kiểm tra xem đơn hàng có đúng là của user đang request không
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("error.order.unauthorized_access");
        }

        // Kiểm tra phương thức thanh toán phải là COD
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new RuntimeException("error.order.not_cod_method");
        }

        // CẬP NHẬT: Kiểm tra xem đơn hàng đã được thanh toán chưa dựa vào paymentStatus
        if (!"UNPAID".equals(order.getPaymentStatus())) {
            throw new RuntimeException("error.order.already_processed");
        }

        // CẬP NHẬT: Đổi trạng thái thanh toán và lưu lại
        order.setPaymentStatus("PAID");
        return orderRepository.save(order);
    }
}