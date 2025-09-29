package com.whiskey.domain.review.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.whiskey.domain.member.QMember;
import com.whiskey.domain.review.QReview;
import com.whiskey.domain.review.Review;
import com.whiskey.domain.review.enums.ReviewFilter;
import com.whiskey.domain.whiskey.QWhiskey;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ReviewRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Review> reviews(long whiskeyId, Pageable pageable) {
         QReview review = QReview.review;
         QMember member = QMember.member;
         QWhiskey whiskey = QWhiskey.whiskey;

        List<Review> reviews = queryFactory
            .selectFrom(review)
            .join(review.member, member).fetchJoin()
            .join(review.whiskey, whiskey).fetchJoin()
            .where(review.whiskey.id.eq(whiskeyId))
            .orderBy(review.createAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(review.count())
            .from(review)
            .where(review.whiskey.id.eq(whiskeyId))
            .fetchOne();

        return new PageImpl<>(reviews, pageable, total);
    }

    @Override
    public List<Review> findLatestReviews(long whiskeyId, Long cursorId, int size, ReviewFilter filter) {
        QReview review = QReview.review;
        QMember member = QMember.member;
        QWhiskey whiskey = QWhiskey.whiskey;

        JPAQuery<Review> query = queryFactory
            .selectFrom(review)
            .join(review.member, member).fetchJoin()
            .join(review.whiskey, whiskey).fetchJoin()
            .where(
                review.whiskey.id.eq(whiskeyId),
                reviewCondition(filter)
            );

        if(cursorId != null) {
            query.where(review.id.lt(cursorId));
        }

        return query.orderBy(review.id.desc()).limit(size + 1).fetch();
    }

    private BooleanExpression reviewCondition(ReviewFilter filter) {
        return switch(filter) {
            case ACTIVE -> QReview.review.deletedAt.isNull();
            case DELETED -> QReview.review.deletedAt.isNotNull();
            case ALL -> null;
        };
    }
}