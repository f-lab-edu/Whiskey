package com.whiskey.aspect;

import com.whiskey.annotation.ActivityLog;
import com.whiskey.domain.log.dto.ActivityLogDto;
import com.whiskey.domain.log.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLogAspect {

    private final ActivityLogService activityLogService;

    private static final String PARAMETER_ID = "id";

    @Around("@annotation(activityLog)")
    public Object activityLogPointcut(ProceedingJoinPoint joinPoint, ActivityLog activityLog) throws Throwable {
        log.info("ActivityLogPointcut");

        // 이게 있어야 api가 실행됨!
        Object result = joinPoint.proceed();

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();
            String[] parameterNames = signature.getParameterNames();
            Class<?>[] parameterTypes = signature.getParameterTypes();

            Long targetId = null;
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(PARAMETER_ID)
                    && parameterTypes[i].equals(Long.class)
                    && targetId == null) {
                    targetId = (Long) args[i];
                    break;
                }
            }

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            ActivityLogDto logDto = ActivityLogDto.of(
                activityLog.type(),
                activityLog.target(),
                targetId,
                request.getRemoteAddr()
            );

            activityLogService.saveAsync(logDto);
            log.info("활동 로그 처리 완료 - 타입: {}, 대상: {}, ID: {}", activityLog.type(), activityLog.target(), targetId);
        }
        catch (Exception e) {
            log.error("활동 로그 AOP 처리 실패", e);
        }

        return result;
    }
}
