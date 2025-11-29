package com.whiskey.whiskey.dto;

import com.whiskey.domain.whiskey.enums.MaltType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record WhiskeySearchRequest(
    String distillery,
    String name,
    String country,
    Integer age,
    MaltType maltType,
    Double abv,
    Integer volume,
    String description,
    Long cursor,
    Integer size
) {
    public WhiskeySearchRequest {
        if(size == null) {
            size = 7;
        }

        if(cursor != null && cursor < 0) {
            throw new IllegalArgumentException("cursor는 0보다 커야합니다.");
        }
    }
}