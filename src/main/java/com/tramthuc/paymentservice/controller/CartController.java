package com.tramthuc.paymentservice.controller;

import com.tramthuc.paymentservice.dto.request.CartItemDto;
import com.tramthuc.paymentservice.dto.response.ApiResponse;
import com.tramthuc.paymentservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // Lấy giỏ hàng hiện tại
    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemDto>>> getCart(
            @RequestHeader(value = "X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy giỏ hàng thành công", cartService.getCart(userId)));
    }

    // API cực kỳ quan trọng: Gộp giỏ hàng Local (khi chưa đăng nhập) với giỏ hàng Redis (khi vừa đăng nhập)
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<List<CartItemDto>>> syncCart(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestBody List<CartItemDto> localCart) {
        
        List<CartItemDto> backendCart = cartService.getCart(userId);
        
        // Thuật toán gộp: Nếu trùng sản phẩm thì cộng dồn số lượng, nếu chưa có thì thêm mới
        for (CartItemDto localItem : localCart) {
            boolean exists = false;
            for (CartItemDto backendItem : backendCart) {
                if (backendItem.getProductId().equals(localItem.getProductId())) {
                    backendItem.setQuantity(backendItem.getQuantity() + localItem.getQuantity());
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                backendCart.add(localItem);
            }
        }
        
        cartService.saveCart(userId, backendCart);
        return ResponseEntity.ok(ApiResponse.success("Đồng bộ giỏ hàng thành công", backendCart));
    }

    // Cập nhật lại giỏ hàng (mỗi khi Frontend thêm/bớt/xóa món, nó sẽ ném cục mảng mới xuống đây)
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> saveCart(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestBody List<CartItemDto> cart) {
        cartService.saveCart(userId, cart);
        return ResponseEntity.ok(ApiResponse.success("Đã lưu giỏ hàng", null));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearCart(
            @RequestHeader(value = "X-User-Id") Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã làm sạch giỏ hàng", null));
    }
}