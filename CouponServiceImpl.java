package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.CouponRequest;
import com.fooddelivery.dto.response.*;
import com.fooddelivery.entity.Coupon;
import com.fooddelivery.exception.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepo;
    private final CouponUsageRepository couponUsageRepo;

    @Override
    @Transactional
    public CouponResponse create(CouponRequest req) {
        if (couponRepo.findByCodeIgnoreCase(req.getCode()).isPresent())
            throw new DuplicateException("Coupon code already exists: " + req.getCode());
        Coupon coupon = buildCoupon(new Coupon(), req);
        return toResponse(couponRepo.save(coupon));
    }

    @Override
    public List<CouponResponse> listActive() {
        return couponRepo.findAll().stream()
                .filter(c -> c.isActive()
                        && !c.getValidFrom().isAfter(LocalDateTime.now())
                        && c.getValidUntil().isAfter(LocalDateTime.now()))
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<CouponResponse> listAll() {
        return couponRepo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CouponResponse update(Long id, CouponRequest req) {
        Coupon coupon = findById(id);
        return toResponse(couponRepo.save(buildCoupon(coupon, req)));
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Coupon coupon = findById(id);
        coupon.setActive(!coupon.isActive());
        couponRepo.save(coupon);
    }

    @Override
    public CouponValidateResponse validate(String code, Long userId, BigDecimal orderAmount) {
        Coupon coupon = getCouponByCode(code);

        if (!coupon.isActive())
            throw new BusinessException("Coupon is not active");
        if (LocalDateTime.now().isBefore(coupon.getValidFrom()))
            throw new BusinessException("Coupon is not yet valid");
        if (LocalDateTime.now().isAfter(coupon.getValidUntil()))
            throw new BusinessException("Coupon has expired");
        if (coupon.getUsedCount() >= coupon.getUsageLimit())
            throw new BusinessException("Coupon usage limit reached");
        if (couponUsageRepo.existsByCouponIdAndUserId(coupon.getId(), userId))
            throw new BusinessException("You have already used this coupon");
        if (orderAmount == null || orderAmount.signum() < 0)
            throw new BusinessException("Order amount must be zero or greater");
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0)
            throw new BusinessException("Minimum order amount for this coupon is ₹" + coupon.getMinOrderAmount());

        BigDecimal discount = calculateDiscount(coupon, orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discount).max(BigDecimal.ZERO);

        return CouponValidateResponse.builder()
                .couponId(coupon.getId()).code(coupon.getCode())
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .discountAmount(discount).finalAmount(finalAmount)
                .build();
    }

    @Override
    public Coupon getCouponByCode(String code) {
        return couponRepo.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + code));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount;
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null)
                discount = discount.min(coupon.getMaxDiscount());
        } else {
            discount = coupon.getDiscountValue();
        }
        return discount.min(orderAmount);
    }

    private Coupon buildCoupon(Coupon c, CouponRequest req) {
        if (req.getValidUntil().isBefore(req.getValidFrom()))
            throw new BusinessException("Coupon end date must be after its start date");
        Coupon.DiscountType discountType;
        try {
            discountType = Coupon.DiscountType.valueOf(req.getDiscountType().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Discount type must be PERCENTAGE or FLAT");
        }
        if (discountType == Coupon.DiscountType.PERCENTAGE
                && req.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0)
            throw new BusinessException("Percentage discount cannot exceed 100");
        c.setCode(req.getCode().toUpperCase(Locale.ROOT));
        c.setDescription(req.getDescription());
        c.setDiscountType(discountType);
        c.setDiscountValue(req.getDiscountValue());
        c.setMinOrderAmount(req.getMinOrderAmount() != null ? req.getMinOrderAmount() : BigDecimal.ZERO);
        c.setMaxDiscount(req.getMaxDiscount());
        c.setUsageLimit(req.getUsageLimit() != null ? req.getUsageLimit() : 1);
        c.setValidFrom(req.getValidFrom());
        c.setValidUntil(req.getValidUntil());
        c.setActive(true);
        return c;
    }

    private Coupon findById(Long id) {
        return couponRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", id));
    }

    private CouponResponse toResponse(Coupon c) {
        return CouponResponse.builder()
                .id(c.getId()).code(c.getCode()).description(c.getDescription())
                .discountType(c.getDiscountType().name()).discountValue(c.getDiscountValue())
                .minOrderAmount(c.getMinOrderAmount()).maxDiscount(c.getMaxDiscount())
                .usageLimit(c.getUsageLimit()).usedCount(c.getUsedCount())
                .validFrom(c.getValidFrom()).validUntil(c.getValidUntil())
                .active(c.isActive()).build();
    }
}
