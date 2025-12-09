package com.whiskey.domain.whiskey.dto;

import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.enums.MaltType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record WhiskeyInfo(
    long id,
    String distillery,
    String name,
    String country,
    Integer age,
    MaltType  maltType,
    double abv,
    String description,
    String imagePath,
    List<String> casks
) {
    public static WhiskeyInfo from(Whiskey whiskey) {
        List<String> casks = whiskey.getCasks()
            .stream()
            .filter(caskInfo -> caskInfo.getType() != null)
            .map(caskInfo -> caskInfo.getType().name())
            .collect(Collectors.toList());

        return new WhiskeyInfo(
            whiskey.getId(),
            whiskey.getDistillery(),
            whiskey.getName(),
            whiskey.getCountry(),
            whiskey.getAge(),
            whiskey.getMaltType(),
            whiskey.getAbv(),
            whiskey.getDescription(),
            whiskey.getImagePath(),
            casks
        );
    }

    public static List<WhiskeyInfo> from(List<Whiskey> whiskeys) {
        List<WhiskeyInfo> responses = new ArrayList<>();

        for(Whiskey whiskey : whiskeys) {
            WhiskeyInfo response = from(whiskey);
            responses.add(response);
        }

        return responses;
    }
}