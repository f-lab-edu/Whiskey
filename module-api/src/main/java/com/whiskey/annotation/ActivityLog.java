package com.whiskey.annotation;

import com.whiskey.domain.log.enums.ActivityType;
import com.whiskey.domain.log.enums.TargetType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActivityLog {
    ActivityType type();
    TargetType target();
    String targetId() default "#id";
}