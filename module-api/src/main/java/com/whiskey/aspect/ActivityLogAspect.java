package com.whiskey.aspect;

import com.whiskey.annotation.ActivityLog;
import com.whiskey.annotation.TargetId;
import com.whiskey.domain.log.dto.ActivityLogDto;
import com.whiskey.domain.log.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLogAspect {

    private final ActivityLogService activityLogService;

    @Around("@annotation(activityLog)")
    public Object activityLogPointcut(ProceedingJoinPoint joinPoint, ActivityLog activityLog) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();

            Long targetId = getTargetId(signature, args);
            Long memberId = getMemberId();

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            ActivityLogDto logDto = ActivityLogDto.of(
                memberId,
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

    private Long getTargetId(MethodSignature signature, Object[] args) {
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof TargetId) {
                    Object arg = args[i];
                    if (arg instanceof Long) {
                        return (Long) arg;
                    }

                    return null;
                }
            }
        }

        return null;
    }

    private Long getMemberId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return null;
            }

            return Long.parseLong(authentication.getName());
        }
        catch (Exception e) {
            return null;
        }
    }
}