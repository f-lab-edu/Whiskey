package com.whiskey.domain.log.service;

import com.whiskey.domain.log.ActivityLog;
import com.whiskey.domain.log.dto.ActivityLogDto;
import com.whiskey.domain.log.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Async("logExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAsync(ActivityLogDto activityLogDto) {
        try {
            log.info("활동 로그 저장 시작");

            ActivityLog logInfo = ActivityLog.builder()
                .memberId(activityLogDto.memberId())
                .activityType(activityLogDto.activityType())
                .targetType(activityLogDto.targetType())
                .targetId(activityLogDto.targetId())
                .ipAddress(activityLogDto.ipAddress())
                .build();

            activityLogRepository.save(logInfo);
        }
        catch (Exception e) {
            log.error("활동 로그 저장 실패", e);
        }
    }
}
