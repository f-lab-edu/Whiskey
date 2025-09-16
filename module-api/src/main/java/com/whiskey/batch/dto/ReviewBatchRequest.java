package com.whiskey.batch.dto;

import lombok.Builder;

@Builder
public record ReviewBatchRequest(
    long whiskeyId,
    long memberId,
    int starRate,
    String content
) {

    public static ReviewBatchRequest of(long whiskeyId, long memberId) {
        return ReviewBatchRequest.builder()
            .whiskeyId(whiskeyId)
            .memberId(memberId)
            .build();
    }
}