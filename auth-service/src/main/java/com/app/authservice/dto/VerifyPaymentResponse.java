package com.app.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyPaymentResponse {

    private boolean verified;

    private String message;
}
