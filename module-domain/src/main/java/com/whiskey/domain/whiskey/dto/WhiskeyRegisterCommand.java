package com.whiskey.domain.whiskey.dto;

import com.whiskey.domain.whiskey.enums.CaskType;
import com.whiskey.domain.whiskey.enums.MaltType;
import java.util.List;

public record WhiskeyRegisterCommand(
    String distillery,
    String name,
    String country,
    Integer age,
    MaltType maltType,
    double abv,
    int volume,
    String description,
    List<CaskRegisterCommand> casks
) {}