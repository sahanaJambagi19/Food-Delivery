package com.fooddelivery.repository;

import com.fooddelivery.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    boolean existsByAddressId(Long addressId);
    List<Order> findByRestaurantIdAndStatus(Long restaurantId, Order.OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o WHERE o.paymentStatus='PAID'")
    BigDecimal totalRevenue();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status NOT IN ('CANCELLED','PAYMENT_FAILED')")
    long countActive();
}
