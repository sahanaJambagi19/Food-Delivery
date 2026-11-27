package com.fooddelivery.repository;

import com.fooddelivery.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant,Long> {
    Optional<Restaurant> findByOwnerId(Long ownerId);

    @Query("SELECT r FROM Restaurant r WHERE r.approved=true AND r.active=true AND (" +
           "LOWER(r.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(r.cuisineType) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(r.city) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Restaurant> search(String q, Pageable pageable);

    Page<Restaurant> findByApprovedTrueAndActiveTrue(Pageable pageable);
    List<Restaurant> findByApprovedFalse();
    long countByApprovedTrue();
}
