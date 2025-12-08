package com.whiskey.domain.review.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.whiskey.domain.member.QMember;
import com.whiskey.domain.review.QReview;
import com.whiskey.domain.review.Review;
import com.whiskey.domain.review.dto.ReviewCursorRequest;
import com.whiskey.domain.review.enums.ReviewFilter;
import com.whiskey.domain.whiskey.QWhiskey;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ReviewRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<Review> findLatestReviews(ReviewCursorRequest request) {
        QReview review = QReview.review;
        QMember member = QMember.member;
        QWhiskey whiskey = QWhiskey.whiskey;

        JPAQuery<Review> query = queryFactory
            .selectFrom(review)
            .join(review.member, member).fetchJoin()
            .join(review.whiskey, whiskey).fetchJoin()
            .where(
                review.whiskey.id.eq(request.whiskeyId()),
                reviewCondition(request.filter())
            );

        if(request.cursor() != null) {
            query.where(review.id.lt(request.getCursorId()));
        }

        return query.orderBy(review.id.desc()).limit(request.size() + 1).fetch();
    }

    @Override
    public List<Review> findByHighestRating(ReviewCursorRequest request) {
        QReview review = QReview.review;
        QMember member = QMember.member;
        QWhiskey whiskey = QWhiskey.whiskey;

        JPAQuery<Review> query = queryFactory
            .selectFrom(review)
            .join(review.member, member).fetchJoin()
            .join(review.whiskey, whiskey).fetchJoin()
            .where(
                review.whiskey.id.eq(request.whiskeyId()),
                reviewCondition(request.filter()),
                ratingHighCursorCondition(request)
            )
            .orderBy(
                review.starRate.desc(),
                review.id.desc()
            )
            .limit(request.size() + 1);

        return query.fetch();
    }

    @Override
    public List<Review> findByLowestRating(ReviewCursorRequest request) {
        QReview review = QReview.review;
        QMember member = QMember.member;
        QWhiskey whiskey = QWhiskey.whiskey;

        JPAQuery<Review> query = queryFactory
            .selectFrom(review)
            .join(review.member, member).fetchJoin()
            .join(review.whiskey, whiskey).fetchJoin()
            .where(
                review.whiskey.id.eq(request.whiskeyId()),
                reviewCondition(request.filter()),
                ratingLowCursorCondition(request)
            )
            .orderBy(
                review.starRate.asc(),
                review.id.desc()
            )
            .limit(request.size() + 1);

        return query.fetch();
    }

    private BooleanExpression reviewCondition(ReviewFilter filter) {
        return switch(filter) {
            case ACTIVE -> QReview.review.deletedAt.isNull();
            case DELETED -> QReview.review.deletedAt.isNotNull();
            case ALL -> null;
        };
    }

    private BooleanExpression ratingHighCursorCondition(ReviewCursorRequest request) {
        if(request.cursor() == null) {
            return null;
        }

        Integer cursorRating = request.getCursorRating();
        Long cursorId = request.getCursorId();

        QReview review = QReview.review;

        return review.starRate.lt(cursorRating).or(review.starRate.eq(cursorRating).and(review.id.lt(cursorId)));
    }

    private BooleanExpression ratingLowCursorCondition(ReviewCursorRequest request) {
        if(request.cursor() == null) {
            return null;
        }

        Integer cursorRating = request.getCursorRating();
        Long cursorId = request.getCursorId();

        QReview review = QReview.review;

        return review.starRate.eq(cursorRating).or(review.starRate.eq(cursorRating).and(review.id.lt(cursorId)));
    }
}