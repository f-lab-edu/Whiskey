package com.whiskey.domain.log;

import com.whiskey.domain.base.BaseEntity;
import com.whiskey.domain.log.enums.ActivityType;
import com.whiskey.domain.log.enums.TargetType;
import com.whiskey.domain.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
public class ActivityLog extends BaseEntity {

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    private String ipAddress;

    @Builder
    public ActivityLog(Long memberId, ActivityType activityType, TargetType targetType, Long targetId, String ipAddress) {
        this.memberId = memberId;
        this.activityType = activityType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.ipAddress = ipAddress;
    }
}
