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
    @Min(7) @Max(50)
    Integer size
) {
    public WhiskeySearchRequest {
        if(cursor != null && cursor < 0) {
            throw new IllegalArgumentException("cursor는 0보다 커야합니다.");
        }
    }
}