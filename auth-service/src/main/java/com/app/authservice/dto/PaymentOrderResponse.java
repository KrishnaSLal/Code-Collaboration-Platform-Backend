package com.app.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentOrderResponse {

    private String keyId;

    private String orderId;

    private int amount;

    private String currency;

    private String receipt;
}
