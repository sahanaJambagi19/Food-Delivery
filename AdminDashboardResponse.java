package com.fooddelivery.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder
public class AdminDashboardResponse {
    private long totalUsers;
    private long totalRestaurants;
    private long totalOrders;
    private long activeOrders;
    private long pendingApprovals;
    private BigDecimal totalRevenue;
    private long totalCustomers;
    private long totalDeliveryPartners;
}
