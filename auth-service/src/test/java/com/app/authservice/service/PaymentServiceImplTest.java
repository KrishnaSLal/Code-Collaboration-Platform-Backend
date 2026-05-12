package com.app.authservice.service;

import com.app.authservice.dto.CreatePaymentOrderRequest;
import com.app.authservice.dto.VerifyPaymentRequest;
import com.app.authservice.dto.VerifyPaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceImplTest {

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl();
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_secret");
    }

    @Test
    void verifyPaymentReturnsVerifiedForMatchingSignature() {
        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_456");
        request.setRazorpaySignature(hmacSha256("order_123|pay_456", "test_secret"));

        VerifyPaymentResponse response = paymentService.verifyPayment(request);

        assertThat(response.isVerified()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Payment verified successfully.");
    }

    @Test
    void verifyPaymentReturnsFalseForMismatchedSignature() {
        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_456");
        request.setRazorpaySignature("not-a-valid-signature");

        VerifyPaymentResponse response = paymentService.verifyPayment(request);

        assertThat(response.isVerified()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Payment verification failed.");
    }

    @Test
    void createOrderFailsFastWhenRazorpayCredentialsAreMissing() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "");

        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest();
        request.setAmount(1000);
        request.setCurrency("INR");

        assertThatThrownBy(() -> paymentService.createOrder(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte item : hash) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
