package com.whiskey.admin.whiskey.dto;

import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.enums.MaltType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record WhiskeyResponseDto(
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
    public static WhiskeyResponseDto from(Whiskey whiskey) {
        List<String> cask = whiskey.getCasks()
            .stream()
            .filter(caskInfo -> caskInfo.getType() != null)
            .map(caskInfo -> caskInfo.getType().name())
            .collect(Collectors.toList());

        return new WhiskeyResponseDto(
            whiskey.getDistillery(),
            whiskey.getName(),
            whiskey.getCountry(),
            whiskey.getAge(),
            whiskey.getMaltType(),
            whiskey.getAbv(),
            whiskey.getDescription(),
            whiskey.getImagePath(),
            cask
        );
    }

    public static List<WhiskeyResponseDto> from(List<Whiskey> whiskeys) {
        List<WhiskeyResponseDto> responses = new ArrayList<>();

        for(Whiskey whiskey : whiskeys) {
            WhiskeyResponseDto response = from(whiskey);
            responses.add(response);
        }

        return responses;
    }
}