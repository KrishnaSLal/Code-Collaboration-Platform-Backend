package com.app.authservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePaymentOrderRequest {

    @Min(100)
    private int amount;

    @NotBlank
    private String currency = "INR";

    private String planName = "CodeSync Pro";

    private Long userId;
}
