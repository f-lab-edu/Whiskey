package com.whiskey.domain.log.dto;

import com.whiskey.domain.log.enums.ActivityType;
import com.whiskey.domain.log.enums.TargetType;

public record ActivityLogDto(
    Long memberId,
    ActivityType activityType,
    TargetType targetType,
    Long targetId,
    String ipAddress
) {
    public static ActivityLogDto of(Long memberId, ActivityType activityType, TargetType targetType, Long targetId, String ipAddress) {
        return new ActivityLogDto(
            memberId,
            activityType,
            targetType,
            targetId,
            ipAddress
        );
    }
}