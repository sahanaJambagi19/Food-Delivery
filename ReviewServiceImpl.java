package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.ReviewRequest;
import com.fooddelivery.dto.response.ReviewResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.exception.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepo;
    private final OrderRepository orderRepo;
    private final RestaurantRepository restaurantRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional
    public ReviewResponse addReview(Long userId, ReviewRequest req) {
        Order order = orderRepo.findById(req.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", req.getOrderId()));

        if (!order.getUser().getId().equals(userId))
            throw new BusinessException("Order does not belong to this user");
        if (order.getStatus() != Order.OrderStatus.DELIVERED)
            throw new BusinessException("You can only review delivered orders");
        if (reviewRepo.existsByOrderId(req.getOrderId()))
            throw new DuplicateException("You have already reviewed this order");

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Review review = Review.builder()
                .user(user).restaurant(order.getRestaurant())
                .order(order).rating(req.getRating()).comment(req.getComment())
                .build();
        reviewRepo.save(review);

        // Recalculate restaurant average rating
        updateRestaurantRating(order.getRestaurant().getId());

        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getRestaurantReviews(Long restaurantId) {
        return reviewRepo.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void updateRestaurantRating(Long restaurantId) {
        Double avg = reviewRepo.avgRatingByRestaurant(restaurantId);
        long count = reviewRepo.countByRestaurantId(restaurantId);
        restaurantRepo.findById(restaurantId).ifPresent(r -> {
            r.setAvgRating(avg != null
                    ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            r.setTotalRatings((int) count);
            restaurantRepo.save(r);
        });
    }

    private ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId()).restaurantId(r.getRestaurant().getId())
                .restaurantName(r.getRestaurant().getName())
                .userId(r.getUser().getId()).userName(r.getUser().getName())
                .rating(r.getRating()).comment(r.getComment())
                .createdAt(r.getCreatedAt()).build();
    }
}
