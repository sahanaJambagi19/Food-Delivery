package com.fooddelivery.service;

import com.fooddelivery.dto.request.*;
import com.fooddelivery.dto.response.*;
import java.util.List;

public interface MenuService {
    // Categories
    FoodCategoryResponse addCategory(Long ownerId, FoodCategoryRequest request);
    List<FoodCategoryResponse> getCategories(Long restaurantId);
    List<FoodCategoryResponse> getOwnerCategories(Long ownerId);
    FoodCategoryResponse updateCategory(Long ownerId, Long categoryId, FoodCategoryRequest request);
    void deleteCategory(Long ownerId, Long categoryId);

    // Food Items
    FoodItemResponse addFoodItem(Long ownerId, FoodItemRequest request);
    List<FoodItemResponse> getFoodItems(Long restaurantId);
    List<FoodItemResponse> getOwnerFoodItems(Long ownerId);
    List<FoodItemResponse> getFoodItemsByCategory(Long restaurantId, Long categoryId);
    FoodItemResponse updateFoodItem(Long ownerId, Long itemId, FoodItemRequest request);
    void toggleAvailability(Long ownerId, Long itemId);
    void deleteFoodItem(Long ownerId, Long itemId);
}
