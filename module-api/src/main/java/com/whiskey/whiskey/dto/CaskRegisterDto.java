package com.whiskey.whiskey.dto;

import com.whiskey.domain.whiskey.enums.CaskType;
import jakarta.validation.constraints.NotNull;

public record CaskRegisterDto(
    @NotNull
    CaskType type
) {}