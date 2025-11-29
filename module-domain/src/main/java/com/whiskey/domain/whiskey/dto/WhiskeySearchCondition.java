package com.whiskey.domain.whiskey.dto;

import com.whiskey.domain.whiskey.enums.MaltType;

public record WhiskeySearchCondition(
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
) {}