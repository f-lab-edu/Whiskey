package com.whiskey.domain.whiskey.dto;

import com.whiskey.domain.whiskey.enums.CaskType;

public record CaskRegisterCommand(
    CaskType type
) {}