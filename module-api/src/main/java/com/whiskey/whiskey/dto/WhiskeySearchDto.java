package com.whiskey.whiskey.dto;

import com.whiskey.domain.whiskey.enums.MaltType;

public record WhiskeySearchDto(
    String distillery,
    String name,
    String country,
    Integer age,
    MaltType maltType,
    Double abv,
    Integer volume,
    String description
) {}