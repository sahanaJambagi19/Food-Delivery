package com.fooddelivery.repository;

import com.fooddelivery.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment,Long> {
    Optional<DeliveryAssignment> findByOrderId(Long orderId);
    List<DeliveryAssignment> findByDeliveryPartnerId(Long partnerId);
    List<DeliveryAssignment> findByDeliveryPartnerIdAndStatus(Long partnerId, DeliveryAssignment.AssignmentStatus status);
}
