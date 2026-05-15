package com.app.authservice.controller;

import com.app.authservice.dto.CreatePaymentOrderRequest;
import com.app.authservice.dto.PaymentOrderResponse;
import com.app.authservice.dto.VerifyPaymentRequest;
import com.app.authservice.dto.VerifyPaymentResponse;
import com.app.authservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders")
    public PaymentOrderResponse createOrder(@Valid @RequestBody CreatePaymentOrderRequest request) {
        return paymentService.createOrder(request);
    }

    @PostMapping("/verify")
    public VerifyPaymentResponse verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        return paymentService.verifyPayment(request);
    }
}
