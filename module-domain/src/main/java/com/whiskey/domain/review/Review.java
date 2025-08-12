package com.whiskey.domain.review;

import com.whiskey.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseEntity {
    @Column(nullable = false, name = "whiskey_id")
    private long whiskeyId;

    @Column(nullable = false, name = "member_id")
    private long memberId;

    @Column(name = "star_rate")
    private int starRate;

    @Column
    private String content;

    @CreatedDate
    @Column(name = "review_date")
    private LocalDateTime reviewDate;
}