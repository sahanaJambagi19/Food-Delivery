package com.fooddelivery.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @Builder
public class CartResponse {
    private Long cartId;
    private Long restaurantId;
    private String restaurantName;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private int totalItems;

    @Getter @Setter @Builder
    public static class CartItemResponse {
        private Long cartItemId;
        private Long foodItemId;
        private String foodItemName;
        private String imageUrl;
        private boolean veg;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}
