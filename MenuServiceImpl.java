package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.*;
import com.fooddelivery.dto.response.*;
import com.fooddelivery.entity.*;
import com.fooddelivery.exception.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final RestaurantRepository restaurantRepo;
    private final FoodCategoryRepository categoryRepo;
    private final FoodItemRepository foodItemRepo;

    // ── Categories ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public FoodCategoryResponse addCategory(Long ownerId, FoodCategoryRequest req) {
        Restaurant restaurant = getOwnerRestaurant(ownerId);
        FoodCategory cat = FoodCategory.builder()
                .restaurant(restaurant).name(req.getName())
                .description(req.getDescription())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .active(true).build();
        return toCategoryResponse(categoryRepo.save(cat));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodCategoryResponse> getCategories(Long restaurantId) {
        return categoryRepo.findByRestaurantIdAndActiveTrueOrderByDisplayOrderAsc(restaurantId)
                .stream().map(this::toCategoryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodCategoryResponse> getOwnerCategories(Long ownerId) {
        Restaurant restaurant = getOwnerRestaurant(ownerId);
        return categoryRepo.findByRestaurantId(restaurant.getId()).stream()
                .map(this::toCategoryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FoodCategoryResponse updateCategory(Long ownerId, Long categoryId, FoodCategoryRequest req) {
        FoodCategory cat = getOwnerCategory(ownerId, categoryId);
        cat.setName(req.getName());
        cat.setDescription(req.getDescription());
        if (req.getDisplayOrder() != null) cat.setDisplayOrder(req.getDisplayOrder());
        return toCategoryResponse(categoryRepo.save(cat));
    }

    @Override
    @Transactional
    public void deleteCategory(Long ownerId, Long categoryId) {
        FoodCategory cat = getOwnerCategory(ownerId, categoryId);
        cat.setActive(false);
        categoryRepo.save(cat);
    }

    // ── Food Items ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public FoodItemResponse addFoodItem(Long ownerId, FoodItemRequest req) {
        Restaurant restaurant = getOwnerRestaurant(ownerId);
        FoodCategory category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));
        if (!category.getRestaurant().getId().equals(restaurant.getId()))
            throw new BusinessException("Category does not belong to your restaurant");

        FoodItem item = FoodItem.builder()
                .restaurant(restaurant).category(category)
                .name(req.getName()).description(req.getDescription())
                .price(req.getPrice()).imageUrl(req.getImageUrl())
                .veg(req.isVeg()).available(req.isAvailable()).build();
        return toItemResponse(foodItemRepo.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItemResponse> getFoodItems(Long restaurantId) {
        return foodItemRepo.findByRestaurantIdAndAvailableTrue(restaurantId).stream()
                .filter(item -> item.getCategory().isActive())
                .map(this::toItemResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItemResponse> getOwnerFoodItems(Long ownerId) {
        Restaurant restaurant = getOwnerRestaurant(ownerId);
        return foodItemRepo.findByRestaurantId(restaurant.getId()).stream()
                .map(this::toItemResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItemResponse> getFoodItemsByCategory(Long restaurantId, Long categoryId) {
        return foodItemRepo.findByRestaurantIdAndCategoryId(restaurantId, categoryId).stream()
                .filter(item -> item.isAvailable() && item.getCategory().isActive())
                .map(this::toItemResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FoodItemResponse updateFoodItem(Long ownerId, Long itemId, FoodItemRequest req) {
        FoodItem item = getOwnerFoodItem(ownerId, itemId);
        FoodCategory category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));
        if (!category.getRestaurant().getId().equals(item.getRestaurant().getId()))
            throw new BusinessException("Category does not belong to your restaurant");
        if (!category.isActive())
            throw new BusinessException("Cannot assign a food item to an inactive category");
        item.setCategory(category);
        item.setName(req.getName());
        item.setDescription(req.getDescription());
        item.setPrice(req.getPrice());
        item.setImageUrl(req.getImageUrl());
        item.setVeg(req.isVeg());
        item.setAvailable(req.isAvailable());
        return toItemResponse(foodItemRepo.save(item));
    }

    @Override
    @Transactional
    public void toggleAvailability(Long ownerId, Long itemId) {
        FoodItem item = getOwnerFoodItem(ownerId, itemId);
        item.setAvailable(!item.isAvailable());
        foodItemRepo.save(item);
    }

    @Override
    @Transactional
    public void deleteFoodItem(Long ownerId, Long itemId) {
        FoodItem item = getOwnerFoodItem(ownerId, itemId);
        // Preserve historical order-item foreign keys; hide the item instead of hard-deleting it.
        item.setAvailable(false);
        foodItemRepo.save(item);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Restaurant getOwnerRestaurant(Long ownerId) {
        return restaurantRepo.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found for this owner"));
    }

    private FoodCategory getOwnerCategory(Long ownerId, Long categoryId) {
        Restaurant r = getOwnerRestaurant(ownerId);
        FoodCategory cat = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        if (!cat.getRestaurant().getId().equals(r.getId()))
            throw new BusinessException("Category does not belong to your restaurant");
        return cat;
    }

    private FoodItem getOwnerFoodItem(Long ownerId, Long itemId) {
        Restaurant r = getOwnerRestaurant(ownerId);
        FoodItem item = foodItemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", itemId));
        if (!item.getRestaurant().getId().equals(r.getId()))
            throw new BusinessException("Food item does not belong to your restaurant");
        return item;
    }

    private FoodCategoryResponse toCategoryResponse(FoodCategory c) {
        return FoodCategoryResponse.builder()
                .id(c.getId()).name(c.getName()).description(c.getDescription())
                .displayOrder(c.getDisplayOrder()).active(c.isActive())
                .restaurantId(c.getRestaurant().getId()).build();
    }

    private FoodItemResponse toItemResponse(FoodItem i) {
        return FoodItemResponse.builder()
                .id(i.getId()).name(i.getName()).description(i.getDescription())
                .price(i.getPrice()).imageUrl(i.getImageUrl()).veg(i.isVeg())
                .available(i.isAvailable()).avgRating(i.getAvgRating())
                .categoryId(i.getCategory().getId()).categoryName(i.getCategory().getName())
                .restaurantId(i.getRestaurant().getId()).restaurantName(i.getRestaurant().getName())
                .build();
    }
}
