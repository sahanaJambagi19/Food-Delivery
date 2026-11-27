package com.fooddelivery.service;

import com.fooddelivery.dto.request.PaymentVerifyRequest;
import com.fooddelivery.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse createRazorpayOrder(Long orderId, Long userId);
    PaymentResponse verifyAndCapture(PaymentVerifyRequest request, Long userId);
    PaymentResponse getPaymentByOrder(Long orderId, Long userId);
}
