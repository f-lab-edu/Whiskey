package com.whiskey.domain.review;

import com.whiskey.domain.base.DeleteEntity;
import com.whiskey.domain.member.Member;
import com.whiskey.domain.whiskey.Whiskey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review extends DeleteEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "whiskey_id")
    private Whiskey whiskey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "star_rate")
    private int starRate;

    @Column
    private String content;

    @CreatedDate
    @Column(name = "review_date")
    private LocalDateTime reviewDate;

    @Builder
    public Review(Whiskey whiskey, Member member, int starRate, String content) {
        this.whiskey = whiskey;
        this.member = member;
        this.starRate = starRate;
        this.content = content;
    }
}