package com.fooddelivery.service;

import com.fooddelivery.dto.request.RestaurantRequest;
import com.fooddelivery.dto.response.*;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface RestaurantService {
    RestaurantResponse register(Long ownerId, RestaurantRequest request);
    RestaurantResponse getById(Long id);
    RestaurantResponse getMyRestaurant(Long ownerId);
    RestaurantResponse update(Long ownerId, RestaurantRequest request);
    void toggleOpen(Long ownerId);

    PageResponse<RestaurantResponse> listApproved(Pageable pageable);
    PageResponse<RestaurantResponse> search(String query, Pageable pageable);

    // Admin
    List<RestaurantResponse> getPendingApprovals();
    RestaurantResponse approve(Long restaurantId);
    RestaurantResponse reject(Long restaurantId);
}
