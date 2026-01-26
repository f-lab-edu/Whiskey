package com.whiskey.domain.whiskey.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.whiskey.domain.whiskey.QCask;
import com.whiskey.domain.whiskey.QWhiskey;
import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.dto.CaskCommand;
import com.whiskey.domain.whiskey.dto.WhiskeyCommand;
import com.whiskey.domain.whiskey.dto.WhiskeySearchCondition;
import com.whiskey.domain.whiskey.enums.CaskType;
import com.whiskey.domain.whiskey.enums.MaltType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@Slf4j
public class WhiskeyRepositoryImpl implements WhiskeyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public WhiskeyRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<Whiskey> searchWhiskeys(WhiskeySearchCondition whiskeyDto) {
        QWhiskey whiskey = QWhiskey.whiskey;

        JPAQuery<Whiskey> query = queryFactory
            .selectFrom(whiskey)
            .where(
                distilleryContains(whiskeyDto.distillery()),
                nameContains(whiskeyDto.name()),
                countryContains(whiskeyDto.country()),
                ageEquals(whiskeyDto.age()),
                maltTypeEquals(whiskeyDto.maltType()),
                volumeEquals(whiskeyDto.volume())
            );

        if(whiskeyDto.cursor() != null) {
            query.where(whiskey.id.lt(whiskeyDto.cursor()));
        }

        return query.orderBy(whiskey.id.desc()).limit(whiskeyDto.size() + 1).fetch();
    }

    @Override
    public int checkDuplicateWhiskey(WhiskeyCommand whiskeyDto) {
        QWhiskey whiskey = QWhiskey.whiskey;
        QCask cask = QCask.cask;

        List<CaskType> caskType = whiskeyDto.casks().stream()
            .map(CaskCommand::type)
            .sorted()
            .toList();

        List<Long> existWhiskeyIds = queryFactory
            .select(whiskey.id)
            .from(whiskey)
            .where(
                distilleryContains(whiskeyDto.distillery()),
                nameContains(whiskeyDto.name()),
                ageEquals(whiskeyDto.age()),
                maltTypeEquals(whiskeyDto.maltType()),
                abvEquals(whiskeyDto.abv()),
                volumeEquals(whiskeyDto.volume())
            ).fetch();

        if(existWhiskeyIds.isEmpty()) {
            return 0;
        }

        boolean checkDuplicate = existWhiskeyIds.stream()
            .anyMatch(whiskeyId -> {
               List<CaskType> existCaskType = queryFactory
                   .select(cask.type)
                   .from(whiskey)
                   .join(whiskey.casks, cask)
                   .where(whiskey.id.eq(whiskeyId))
                   .orderBy(cask.type.asc())
                   .fetch();

               return caskType.equals(existCaskType);
            });

        return checkDuplicate ? 1 : 0;
    }

    private BooleanExpression distilleryContains(String distillery) {
        return StringUtils.hasText(distillery) ? QWhiskey.whiskey.distillery.containsIgnoreCase(distillery) : null;
    }

    private BooleanExpression nameContains(String name) {
        return StringUtils.hasText(name) ? QWhiskey.whiskey.name.containsIgnoreCase(name) : null;
    }

    private BooleanExpression countryContains(String country) {
        return StringUtils.hasText(country) ? QWhiskey.whiskey.country.containsIgnoreCase(country) : null;
    }

    private BooleanExpression ageEquals(Integer age) {
        return age != null ? QWhiskey.whiskey.age.eq(age) : null;
    }

    private BooleanExpression maltTypeEquals(Enum<?> maltType) {
        return maltType != null ? QWhiskey.whiskey.maltType.eq((MaltType) maltType) : null;
    }

    private BooleanExpression abvEquals(Double abv) {
        return abv != null ? QWhiskey.whiskey.abv.eq(abv) : null;
    }

    private BooleanExpression volumeEquals(Integer volume) {
        return volume != null ? QWhiskey.whiskey.volume.eq(volume) : null;
    }
}
