package com.fooddelivery.repository;

import com.fooddelivery.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon,Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
}
