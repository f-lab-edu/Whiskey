package com.whiskey.admin.whiskey.dto;

import com.whiskey.domain.whiskey.enums.MaltType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WhiskeyRegisterDto(
    @NotBlank String distillery,
    @NotBlank String name,
    @NotBlank String country,
    Integer age,
    @NotNull MaltType maltType,
    @NotNull @Min(0) @Max(100) double abv,
    @NotNull @Min(0) int volume,
    @NotBlank String description,
    @NotEmpty @Valid List<CaskRegisterDto> casks) {

}
