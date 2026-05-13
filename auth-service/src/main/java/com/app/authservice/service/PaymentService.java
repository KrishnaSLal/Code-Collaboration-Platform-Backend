package com.app.authservice.service;

import com.app.authservice.dto.CreatePaymentOrderRequest;
import com.app.authservice.dto.PaymentOrderResponse;
import com.app.authservice.dto.VerifyPaymentRequest;
import com.app.authservice.dto.VerifyPaymentResponse;

public interface PaymentService {

    PaymentOrderResponse createOrder(CreatePaymentOrderRequest request);

    VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request);
}
