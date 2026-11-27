package com.fooddelivery.repository;

import com.fooddelivery.entity.FoodCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodCategoryRepository extends JpaRepository<FoodCategory,Long> {
    List<FoodCategory> findByRestaurantIdAndActiveTrueOrderByDisplayOrderAsc(Long restaurantId);
    List<FoodCategory> findByRestaurantId(Long restaurantId);
}
