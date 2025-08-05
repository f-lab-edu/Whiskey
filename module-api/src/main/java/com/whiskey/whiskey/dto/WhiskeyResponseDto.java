package com.whiskey.whiskey.dto;

import com.whiskey.domain.whiskey.dto.WhiskeyInfo;
import com.whiskey.domain.whiskey.enums.MaltType;
import java.util.List;

public record WhiskeyResponseDto(
    long id,
    String distillery,
    String name,
    String country,
    int age,
    MaltType maltType,
    double abv,
    String description,
    String imagePath,
    List<String> casks
) {
    public static WhiskeyResponseDto from(WhiskeyInfo whiskeyInfo) {
        return new WhiskeyResponseDto(
            whiskeyInfo.id(),
            whiskeyInfo.distillery(),
            whiskeyInfo.name(),
            whiskeyInfo.country(),
            whiskeyInfo.age(),
            whiskeyInfo.maltType(),
            whiskeyInfo.abv(),
            whiskeyInfo.description(),
            whiskeyInfo.imagePath(),
            whiskeyInfo.casks()
        );
    }

    public static List<WhiskeyResponseDto> from(List<WhiskeyInfo> whiskeys) {
        return whiskeys.stream()
            .map(WhiskeyResponseDto::from)
            .toList();
    }
}