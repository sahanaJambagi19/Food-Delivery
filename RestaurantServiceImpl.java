package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.RestaurantRequest;
import com.fooddelivery.dto.response.*;
import com.fooddelivery.entity.*;
import com.fooddelivery.exception.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepo;
    private final UserRepository userRepo;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    @Override
    @Transactional
    public RestaurantResponse register(Long ownerId, RestaurantRequest req) {
        if (restaurantRepo.findByOwnerId(ownerId).isPresent())
            throw new DuplicateException("You already have a registered restaurant");
        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));
        Restaurant r = buildRestaurant(new Restaurant(), req);
        r.setOwner(owner);
        return toResponse(restaurantRepo.save(r));
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getById(Long id) {
        return toResponse(findApprovedById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getMyRestaurant(Long ownerId) {
        return toResponse(restaurantRepo.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found for this owner")));
    }

    @Override
    @Transactional
    public RestaurantResponse update(Long ownerId, RestaurantRequest req) {
        Restaurant r = restaurantRepo.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found for this owner"));
        return toResponse(restaurantRepo.save(buildRestaurant(r, req)));
    }

    @Override
    @Transactional
    public void toggleOpen(Long ownerId) {
        Restaurant r = restaurantRepo.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found for this owner"));
        r.setOpen(!r.isOpen());
        restaurantRepo.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RestaurantResponse> listApproved(Pageable pageable) {
        return PageResponse.of(restaurantRepo.findByApprovedTrueAndActiveTrue(pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RestaurantResponse> search(String query, Pageable pageable) {
        return PageResponse.of(restaurantRepo.search(query, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getPendingApprovals() {
        return restaurantRepo.findByApprovedFalse().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RestaurantResponse approve(Long id) {
        Restaurant r = restaurantRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
        r.setApproved(true);
        r.setActive(true);
        return toResponse(restaurantRepo.save(r));
    }

    @Override
    @Transactional
    public RestaurantResponse reject(Long id) {
        Restaurant r = restaurantRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
        r.setApproved(false);
        r.setActive(false);
        return toResponse(restaurantRepo.save(r));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Restaurant buildRestaurant(Restaurant r, RestaurantRequest req) {
        r.setName(req.getName());
        r.setDescription(req.getDescription());
        r.setCuisineType(req.getCuisineType());
        r.setAddress(req.getAddress());
        r.setCity(req.getCity());
        r.setPincode(req.getPincode());
        r.setPhone(req.getPhone());
        r.setEmail(req.getEmail());
        r.setLogoUrl(req.getLogoUrl());
        r.setCoverImageUrl(req.getCoverImageUrl());
        r.setAvgDeliveryTime(req.getAvgDeliveryTime() != null ? req.getAvgDeliveryTime() : 30);
        if (req.getMinOrderAmount() != null) r.setMinOrderAmount(req.getMinOrderAmount());
        if (req.getDeliveryCharge() != null) r.setDeliveryCharge(req.getDeliveryCharge());
        if (req.getOpeningTime() != null) r.setOpeningTime(LocalTime.parse(req.getOpeningTime(), TIME_FMT));
        if (req.getClosingTime() != null) r.setClosingTime(LocalTime.parse(req.getClosingTime(), TIME_FMT));
        return r;
    }

    private Restaurant findApprovedById(Long id) {
        Restaurant r = restaurantRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
        if (!r.isApproved() || !r.isActive())
            throw new ResourceNotFoundException("Restaurant", id);
        return r;
    }

    private RestaurantResponse toResponse(Restaurant r) {
        return RestaurantResponse.builder()
                .id(r.getId()).name(r.getName()).description(r.getDescription())
                .cuisineType(r.getCuisineType()).address(r.getAddress()).city(r.getCity())
                .pincode(r.getPincode()).phone(r.getPhone()).email(r.getEmail())
                .logoUrl(r.getLogoUrl()).coverImageUrl(r.getCoverImageUrl())
                .openingTime(r.getOpeningTime() != null ? r.getOpeningTime().format(TIME_FMT) : null)
                .closingTime(r.getClosingTime() != null ? r.getClosingTime().format(TIME_FMT) : null)
                .avgDeliveryTime(r.getAvgDeliveryTime()).minOrderAmount(r.getMinOrderAmount())
                .deliveryCharge(r.getDeliveryCharge()).avgRating(r.getAvgRating())
                .totalRatings(r.getTotalRatings()).open(r.isOpen())
                .approved(r.isApproved()).active(r.isActive())
                .ownerId(r.getOwner().getId()).ownerName(r.getOwner().getName())
                .build();
    }
}
