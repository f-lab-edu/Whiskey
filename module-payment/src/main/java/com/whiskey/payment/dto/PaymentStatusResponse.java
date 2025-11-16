package com.whiskey.payment.dto;

public record PaymentStatusResponse(
    String status
) {
    public boolean isApproved() {
        return "DONE".equals(status);
    }
}