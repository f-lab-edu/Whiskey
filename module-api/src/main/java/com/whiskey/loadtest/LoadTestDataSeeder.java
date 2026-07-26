package com.whiskey.loadtest;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.enums.MemberRole;
import com.whiskey.domain.member.enums.MemberStatus;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.domain.stock.Stock;
import com.whiskey.domain.stock.repository.StockRepository;
import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.enums.MaltType;
import com.whiskey.domain.whiskey.repository.WhiskeyRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.LongStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이슈 #61 : k6 부하 테스트용 시드 데이터 적재.
 *
 * <p>{@code tests/k6/load-test.js}는 회원 9명 / 위스키 25건 / 재고 25건이 이미 존재한다고 가정하지만
 * Flyway 마이그레이션은 DDL만 담고 있어, 시드가 없으면 {@code setup()}의 로그인부터 전부 실패한다.
 * 이 러너가 {@code loadtest} 프로파일 부팅 시 그 전제를 채운다.</p>
 *
 * <p>Flyway 마이그레이션이 아니라 {@code @Profile("loadtest")} 러너로 구현한 이유는,
 * 마이그레이션에 INSERT를 넣으면 local/prod 부팅에서도 그대로 실행되기 때문이다.</p>
 *
 * <p>멱등성 : 회원은 이메일 단건 조회로, 위스키/재고는 테이블이 비어있을 때만 적재한다.
 * 재기동해도 중복 INSERT나 PK 충돌이 발생하지 않는다.</p>
 */
@Component
@Profile("loadtest")
@RequiredArgsConstructor
@Slf4j
public class LoadTestDataSeeder implements ApplicationRunner {

    private static final int MEMBER_COUNT = 9;
    private static final BigDecimal STOCK_PRICE = new BigDecimal("5000");
    private static final List<Long> K6_STOCK_IDS = List.of(1L, 2L, 3L, 4L, 5L);

    private static final int VOLUME_ML = 700;

    private final MemberRepository memberRepository;
    private final WhiskeyRepository whiskeyRepository;
    private final StockRepository stockRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${loadtest.seed.stock-quantity:10000}")
    private int stockQuantity;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[loadtest-seed] 시드 데이터 확인 시작");

        seedMembers();
        seedWhiskeys();
        seedStocks();
        verifyK6Assumptions();
    }

    /**
     * k6 setup()이 로그인할 9개 계정. 이메일/비밀번호 규칙은 load-test.js 와 동일하게
     * {@code tester11@example.com / tester11@@} … {@code tester99@example.com / tester99@@} 형태다.
     */
    private void seedMembers() {
        int created = 0;

        for (int n = 1; n <= MEMBER_COUNT; n++) {
            String email = "tester%d%d@example.com".formatted(n, n);

            if (memberRepository.findByEmail(email).isPresent()) {
                continue;
            }

            memberRepository.save(Member.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("tester%d%d@@".formatted(n, n)))
                .memberName("부하테스터" + n)
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
                .build());
            created++;
        }

        log.info("[loadtest-seed] 회원 - 신규 {}건 적재 (기대 총 {}건)", created, MEMBER_COUNT);
    }

    private void seedWhiskeys() {
        long existing = whiskeyRepository.count();

        if (existing > 0) {
            log.info("[loadtest-seed] 위스키 - 기존 {}건 존재하여 스킵", existing);
            return;
        }

        List<Whiskey> whiskeys = WHISKEY_SPECS.stream()
            .map(spec -> Whiskey.builder()
                .distillery(spec.distillery())
                .name(spec.name())
                .country(spec.country())
                .age(spec.age())
                .volume(VOLUME_ML)
                .maltType(spec.maltType())
                .abv(spec.abv())
                .build())
            .toList();

        whiskeyRepository.saveAll(whiskeys);
        log.info("[loadtest-seed] 위스키 - 신규 {}건 적재", whiskeys.size());
    }

    private void seedStocks() {
        long existing = stockRepository.count();

        if (existing > 0) {
            log.info("[loadtest-seed] 재고 - 기존 {}건 존재하여 스킵", existing);
            return;
        }

        List<Stock> stocks = whiskeyRepository.findAll().stream()
            .map(whiskey -> Stock.of(whiskey.getId(), stockQuantity, STOCK_PRICE))
            .toList();

        stockRepository.saveAll(stocks);
        log.info("[loadtest-seed] 재고 - 신규 {}건 적재 (수량 {} / 단가 {})", stocks.size(), stockQuantity, STOCK_PRICE);
    }

    /**
     * k6 스크립트는 위스키 id 1~25, 재고 id 1~5를 상수로 박아 쓴다.
     * 비어있지 않은 DB에 시드하면 AUTO_INCREMENT가 어긋나 부하 테스트가 통째로 실패하므로,
     * 조용히 넘어가지 않고 부팅 로그에 남긴다.
     */
    private void verifyK6Assumptions() {
        List<Long> whiskeyIds = whiskeyRepository.findAll().stream().map(Whiskey::getId).toList();
        List<Long> stockIds = stockRepository.findAll().stream().map(Stock::getId).toList();

        List<Long> expectedWhiskeyIds = LongStream.rangeClosed(1, WHISKEY_SPECS.size()).boxed().toList();

        if (whiskeyIds.containsAll(expectedWhiskeyIds) && stockIds.containsAll(K6_STOCK_IDS)) {
            log.info("[loadtest-seed] 시드 완료 - k6 전제(위스키 id 1~{}, 재고 id 1~5) 충족", WHISKEY_SPECS.size());
            return;
        }

        log.warn("[loadtest-seed] k6 전제와 ID가 불일치 - 기대: 위스키 1~{} / 재고 1~5, 실제: 위스키 {} / 재고 {}."
                + " 부하 테스트 전용 DB를 비우고(TRUNCATE) 재기동해야 한다. (tests/k6/load-test.js 기준)",
            WHISKEY_SPECS.size(), whiskeyIds, stockIds);
    }

    private record WhiskeySpec(String distillery, String name, String country, Integer age, MaltType maltType, double abv) {
    }

    /**
     * k6 browse()의 필터 조합(SINGLE_MALT / BOURBON / BLENDED+스코틀랜드 / 스코틀랜드 / 무필터)이
     * 모두 결과를 반환하도록 몰트 타입과 국가를 분산시킨 25건.
     */
    private static final List<WhiskeySpec> WHISKEY_SPECS = List.of(
        // 1~12 : SINGLE_MALT / 스코틀랜드
        new WhiskeySpec("Glenfiddich", "글렌피딕 12년", "스코틀랜드", 12, MaltType.SINGLE_MALT, 40.0),
        new WhiskeySpec("Macallan", "맥캘란 12년 셰리 오크", "스코틀랜드", 12, MaltType.SINGLE_MALT, 40.0),
        new WhiskeySpec("Glenlivet", "글렌리벳 12년", "스코틀랜드", 12, MaltType.SINGLE_MALT, 40.0),
        new WhiskeySpec("Balvenie", "발베니 더블우드 12년", "스코틀랜드", 12, MaltType.SINGLE_MALT, 40.0),
        new WhiskeySpec("Ardbeg", "아드벡 10년", "스코틀랜드", 10, MaltType.SINGLE_MALT, 46.0),
        new WhiskeySpec("Laphroaig", "라프로익 10년", "스코틀랜드", 10, MaltType.SINGLE_MALT, 40.0),
        new WhiskeySpec("Lagavulin", "라가불린 16년", "스코틀랜드", 16, MaltType.SINGLE_MALT, 43.0),
        new WhiskeySpec("Talisker", "탈리스커 10년", "스코틀랜드", 10, MaltType.SINGLE_MALT, 45.8),
        new WhiskeySpec("Highland Park", "하이랜드 파크 12년", "스코틀랜드", 12, MaltType.SINGLE_MALT, 40.0),
        new WhiskeySpec("Oban", "오반 14년", "스코틀랜드", 14, MaltType.SINGLE_MALT, 43.0),
        new WhiskeySpec("Springbank", "스프링뱅크 10년", "스코틀랜드", 10, MaltType.SINGLE_MALT, 46.0),
        new WhiskeySpec("Bowmore", "보모어 12년", "스코틀랜드", 12, MaltType.SINGLE_MALT, 40.0),

        // 13~17 : BLENDED / 스코틀랜드
        new WhiskeySpec("Johnnie Walker", "조니워커 블랙라벨 12년", "스코틀랜드", 12, MaltType.BLENDED, 40.0),
        new WhiskeySpec("Chivas Brothers", "시바스 리갈 12년", "스코틀랜드", 12, MaltType.BLENDED, 40.0),
        new WhiskeySpec("Ballantine's", "발렌타인 17년", "스코틀랜드", 17, MaltType.BLENDED, 40.0),
        new WhiskeySpec("Dewar's", "듀어스 화이트라벨", "스코틀랜드", null, MaltType.BLENDED, 40.0),
        new WhiskeySpec("Edrington", "페이머스 그라우스", "스코틀랜드", null, MaltType.BLENDED, 40.0),

        // 18~22 : BOURBON / 미국
        new WhiskeySpec("Buffalo Trace", "버팔로 트레이스", "미국", null, MaltType.BOURBON, 45.0),
        new WhiskeySpec("Maker's Mark", "메이커스 마크", "미국", null, MaltType.BOURBON, 45.0),
        new WhiskeySpec("Woodford Reserve", "우드포드 리저브", "미국", null, MaltType.BOURBON, 43.2),
        new WhiskeySpec("Wild Turkey", "와일드 터키 101", "미국", null, MaltType.BOURBON, 50.5),
        new WhiskeySpec("Buffalo Trace", "이글 레어 10년", "미국", 10, MaltType.BOURBON, 45.0),

        // 23~25 : 그 외 (무필터 조회 다양성)
        new WhiskeySpec("Jack Daniel's", "잭 다니엘 올드 No.7", "미국", null, MaltType.TENNESSEE_WHISKEY, 40.0),
        new WhiskeySpec("Suntory", "야마자키 12년", "일본", 12, MaltType.SINGLE_MALT, 43.0),
        new WhiskeySpec("Midleton", "제임슨", "아일랜드", null, MaltType.BLENDED, 40.0)
    );
}
