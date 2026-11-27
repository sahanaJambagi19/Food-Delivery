package com.fooddelivery.service;

import com.fooddelivery.dto.request.PlaceOrderRequest;
import com.fooddelivery.dto.response.*;
import com.fooddelivery.entity.Order;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse placeOrder(Long userId, PlaceOrderRequest request);
    OrderResponse getOrder(Long orderId, Long userId);
    PageResponse<OrderResponse> getMyOrders(Long userId, Pageable pageable);

    // Restaurant owner
    PageResponse<OrderResponse> getRestaurantOrders(Long ownerId, Pageable pageable);
    OrderResponse updateStatus(Long ownerId, Long orderId, Order.OrderStatus status, String note);

    // Admin
    PageResponse<OrderResponse> getAllOrders(Pageable pageable);
    OrderResponse cancelOrder(Long orderId, Long userId);
}
