package com.fooddelivery.repository;

import com.fooddelivery.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage,Long> {
    boolean existsByCouponIdAndUserId(Long couponId, Long userId);
}
