package com.fooddelivery.repository;

import com.fooddelivery.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodItemRepository extends JpaRepository<FoodItem,Long> {
    List<FoodItem> findByRestaurantId(Long restaurantId);
    List<FoodItem> findByRestaurantIdAndAvailableTrue(Long restaurantId);
    List<FoodItem> findByCategoryId(Long categoryId);
    List<FoodItem> findByRestaurantIdAndCategoryId(Long restaurantId, Long categoryId);
}
