package com.tramthuc.paymentservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramthuc.paymentservice.dto.request.CartItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper; // Dùng để chuyển đổi List object thành chuỗi JSON và ngược lại
    
    // Tiền tố khóa trong Redis, ví dụ: CART:1 (Giỏ hàng của User ID 1)
    private static final String CART_PREFIX = "CART:";

    // 1. Lấy giỏ hàng của User
    public List<CartItemDto> getCart(Long userId) {
        try {
            String cartJson = redisTemplate.opsForValue().get(CART_PREFIX + userId);
            if (cartJson != null) {
                // Biến chuỗi JSON trong Redis thành List<CartItemDto>
                return objectMapper.readValue(cartJson, new TypeReference<List<CartItemDto>>() {});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>(); // Nếu chưa có gì thì trả về mảng rỗng
    }

    // 2. Lưu toàn bộ giỏ hàng đè lên Redis
    public void saveCart(Long userId, List<CartItemDto> cart) {
        try {
            // Biến List thành chuỗi JSON
            String cartJson = objectMapper.writeValueAsString(cart);
            redisTemplate.opsForValue().set(CART_PREFIX + userId, cartJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 3. Xóa sạch giỏ hàng (Gọi hàm này sau khi Thanh toán thành công)
    public void clearCart(Long userId) {
        redisTemplate.delete(CART_PREFIX + userId);
    }
}