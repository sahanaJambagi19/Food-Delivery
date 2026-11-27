package com.fooddelivery.service;

import com.fooddelivery.dto.request.ReviewRequest;
import com.fooddelivery.dto.response.ReviewResponse;
import java.util.List;

public interface ReviewService {
    ReviewResponse addReview(Long userId, ReviewRequest request);
    List<ReviewResponse> getRestaurantReviews(Long restaurantId);
}
