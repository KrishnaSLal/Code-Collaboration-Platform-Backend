package com.app.authservice.service;

import com.app.authservice.dto.CreatePaymentOrderRequest;
import com.app.authservice.dto.PaymentOrderResponse;
import com.app.authservice.dto.VerifyPaymentRequest;
import com.app.authservice.dto.VerifyPaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String RAZORPAY_ORDERS_URL = "https://api.razorpay.com/v1/orders";

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Override
    public PaymentOrderResponse createOrder(CreatePaymentOrderRequest request) {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new RuntimeException("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }

        String receipt = "codesync_" + System.currentTimeMillis();

        Map<String, Object> body = new HashMap<>();
        body.put("amount", request.getAmount());
        body.put("currency", request.getCurrency());
        body.put("receipt", receipt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(razorpayKeyId, razorpayKeySecret);

        ResponseEntity<Map> response = new RestTemplate().exchange(
                RAZORPAY_ORDERS_URL,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<?, ?> order = response.getBody();
        if (order == null || order.get("id") == null) {
            throw new RuntimeException("Razorpay order creation failed.");
        }

        return PaymentOrderResponse.builder()
                .keyId(razorpayKeyId)
                .orderId(String.valueOf(order.get("id")))
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .receipt(receipt)
                .build();
    }

    @Override
    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {
        if (razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new RuntimeException("Razorpay is not configured. Set RAZORPAY_KEY_SECRET.");
        }

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        String expectedSignature = hmacSha256(payload, razorpayKeySecret);
        boolean verified = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                request.getRazorpaySignature().getBytes(StandardCharsets.UTF_8)
        );

        return VerifyPaymentResponse.builder()
                .verified(verified)
                .message(verified ? "Payment verified successfully." : "Payment verification failed.")
                .build();
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
            throw new RuntimeException("Could not verify Razorpay signature.", exception);
        }
    }
}
