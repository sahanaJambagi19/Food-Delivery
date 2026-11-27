package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.PaymentVerifyRequest;
import com.fooddelivery.dto.response.PaymentResponse;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.Payment;
import com.fooddelivery.exception.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.*;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service @RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepo;
    private final OrderRepository orderRepo;
    private final NotificationService notificationService;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.secret}") private String razorpaySecret;
    @Value("${razorpay.key.id}") private String razorpayKeyId;
    @Value("${app.payment.demo-mode:false}") private boolean demoMode;

    private static final String DEMO_PREFIX = "demo_order_";

    @Override
    @Transactional
    public PaymentResponse createRazorpayOrder(Long orderId, Long userId) {
        Order order = findOrder(orderId);
        validateOwnership(order, userId);

        if (order.getPaymentMethod() == Order.PaymentMethod.COD)
            throw new BusinessException("COD orders do not require online payment");
        if (order.getStatus() == Order.OrderStatus.CANCELLED || order.getStatus() == Order.OrderStatus.REJECTED)
            throw new BusinessException("Payment is not allowed for a cancelled or rejected order");
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID)
            throw new BusinessException("Order is already paid");

        // Allow a customer to retry after a previous payment verification failure.
        if (order.getPaymentStatus() == Order.PaymentStatus.FAILED
                || order.getStatus() == Order.OrderStatus.PAYMENT_FAILED) {
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            order.setStatus(Order.OrderStatus.PLACED);
            orderRepo.save(order);
        }

        if (demoMode) {
            Payment payment = paymentRepo.findByOrderId(orderId).orElse(
                    Payment.builder().order(order).amount(order.getTotalAmount())
                            .currency("INR").build());
            payment.setRazorpayOrderId(DEMO_PREFIX + order.getOrderNumber());
            payment.setStatus(Payment.PaymentStatus.CREATED);
            return toResponse(paymentRepo.save(payment));
        }

        // Amount in paise (Razorpay uses smallest currency unit)
        int amountInPaise;
        try {
            amountInPaise = order.getTotalAmount()
                    .movePointRight(2).intValueExact();
        } catch (ArithmeticException ex) {
            throw new BusinessException("Order amount is outside the supported payment range");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            options.put("receipt", order.getOrderNumber());

            com.razorpay.Order rzpOrder = razorpayClient.orders.create(options);

            Payment payment = paymentRepo.findByOrderId(orderId).orElse(
                    Payment.builder().order(order).amount(order.getTotalAmount())
                            .currency("INR").build());
            payment.setRazorpayOrderId(rzpOrder.get("id"));
            payment.setStatus(Payment.PaymentStatus.CREATED);
            Payment saved = paymentRepo.save(payment);

            return toResponse(saved);
        } catch (RazorpayException e) {
            throw new BusinessException("Failed to create payment order: " + e.getMessage());
        }
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public PaymentResponse verifyAndCapture(PaymentVerifyRequest req, Long userId) {
        Payment payment = paymentRepo.findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for Razorpay order: " + req.getRazorpayOrderId()));

        validateOwnership(payment.getOrder(), userId);

        if (payment.getOrder().getStatus() == Order.OrderStatus.CANCELLED
                || payment.getOrder().getStatus() == Order.OrderStatus.REJECTED)
            throw new BusinessException("This order is no longer eligible for payment");

        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            if (payment.getRazorpayPaymentId() != null
                    && payment.getRazorpayPaymentId().equals(req.getRazorpayPaymentId()))
                return toResponse(payment); // Idempotent retry after a successful callback.
            throw new BusinessException("This payment has already been completed");
        }

        // Verify HMAC-SHA256 signature (skipped only for simulated payments in demo mode)
        boolean demoPayment = demoMode && req.getRazorpayOrderId().startsWith(DEMO_PREFIX);
        if (!demoPayment && !verifySignature(req.getRazorpayOrderId(), req.getRazorpayPaymentId(),
                req.getRazorpaySignature())) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepo.save(payment);
            payment.getOrder().setPaymentStatus(Order.PaymentStatus.FAILED);
            payment.getOrder().setStatus(Order.OrderStatus.PAYMENT_FAILED);
            orderRepo.save(payment.getOrder());
            throw new BusinessException("Payment verification failed. Invalid signature.");
        }

        // Mark payment success
        payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
        payment.setRazorpaySignature(req.getRazorpaySignature());
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepo.save(payment);

        // Update order
        Order order = payment.getOrder();
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        orderRepo.save(order);

        notificationService.send(userId, "Payment Successful ✅",
                "Payment of ₹" + payment.getAmount() + " for order #"
                        + order.getOrderNumber() + " confirmed.", "PAYMENT");

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrder(Long orderId, Long userId) {
        Order order = findOrder(orderId);
        validateOwnership(order, userId);
        Payment payment = paymentRepo.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
        return toResponse(payment);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean verifySignature(String rzpOrderId, String rzpPaymentId, String signature) {
        try {
            String payload = rzpOrderId + "|" + rzpPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    razorpaySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generated = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
                    generated.getBytes(StandardCharsets.US_ASCII),
                    signature.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return false;
        }
    }

    private Order findOrder(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    private void validateOwnership(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId))
            throw new BusinessException("Order does not belong to this user");
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getId()).orderId(p.getOrder().getId())
                .orderNumber(p.getOrder().getOrderNumber())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .amount(p.getAmount()).currency(p.getCurrency())
                .status(p.getStatus().name()).paidAt(p.getPaidAt())
                .demo(demoMode).razorpayKeyId(razorpayKeyId)
                .build();
    }
}
