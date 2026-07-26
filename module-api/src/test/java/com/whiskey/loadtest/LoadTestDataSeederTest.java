package com.whiskey.loadtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.enums.MemberStatus;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.domain.stock.Stock;
import com.whiskey.domain.stock.repository.StockRepository;
import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.enums.MaltType;
import com.whiskey.domain.whiskey.repository.WhiskeyRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class LoadTestDataSeederTest {

    private static final int STOCK_QUANTITY = 10_000;

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final WhiskeyRepository whiskeyRepository = mock(WhiskeyRepository.class);
    private final StockRepository stockRepository = mock(StockRepository.class);

    // 해시가 k6의 평문 비밀번호로 실제 검증되는지 보려면 실제 인코더가 필요하다.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("빈 DB 부팅 시 회원 9 / 위스키 25 / 재고 25를 적재한다")
    void 빈DB_시드적재() {
        givenEmptyDatabase();

        seeder().run(null);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.captor();
        verify(memberRepository, times(9)).save(memberCaptor.capture());

        ArgumentCaptor<List<Whiskey>> whiskeyCaptor = ArgumentCaptor.captor();
        verify(whiskeyRepository).saveAll(whiskeyCaptor.capture());
        assertThat(whiskeyCaptor.getValue()).hasSize(25);

        ArgumentCaptor<List<Stock>> stockCaptor = ArgumentCaptor.captor();
        verify(stockRepository).saveAll(stockCaptor.capture());
        assertThat(stockCaptor.getValue()).hasSize(25);
    }

    @Test
    @DisplayName("적재된 계정은 k6 setup()의 이메일/비밀번호 규칙으로 로그인 가능하다")
    void 계정규칙_k6일치() {
        givenEmptyDatabase();

        seeder().run(null);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.captor();
        verify(memberRepository, times(9)).save(memberCaptor.capture());
        List<Member> members = memberCaptor.getAllValues();

        assertThat(members).extracting(Member::getEmail)
            .containsExactly(
                "tester11@example.com", "tester22@example.com", "tester33@example.com",
                "tester44@example.com", "tester55@example.com", "tester66@example.com",
                "tester77@example.com", "tester88@example.com", "tester99@example.com");

        assertThat(members).allSatisfy(member -> assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE));

        // k6가 보내는 평문 비밀번호가 저장된 해시와 실제로 매칭되어야 AuthService.login 이 통과한다.
        assertThat(passwordEncoder.matches("tester11@@", members.get(0).getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("tester99@@", members.get(8).getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("적재된 위스키는 k6 browse()의 모든 필터 조합에서 결과를 반환한다")
    void 위스키_k6필터조합_충족() {
        givenEmptyDatabase();

        seeder().run(null);

        ArgumentCaptor<List<Whiskey>> whiskeyCaptor = ArgumentCaptor.captor();
        verify(whiskeyRepository).saveAll(whiskeyCaptor.capture());
        List<Whiskey> whiskeys = whiskeyCaptor.getValue();

        assertThat(whiskeys).anyMatch(w -> w.getMaltType() == MaltType.SINGLE_MALT);
        assertThat(whiskeys).anyMatch(w -> w.getMaltType() == MaltType.BOURBON);
        assertThat(whiskeys).anyMatch(w -> "스코틀랜드".equals(w.getCountry()));
        assertThat(whiskeys).anyMatch(w -> w.getMaltType() == MaltType.BLENDED && "스코틀랜드".equals(w.getCountry()));
    }

    @Test
    @DisplayName("적재된 재고는 단가 5000 / 수량 5000 이상이라 k6 purchase()의 결제 금액과 물량을 감당한다")
    void 재고_k6결제금액_일치() {
        givenEmptyDatabase();

        seeder().run(null);

        ArgumentCaptor<List<Stock>> stockCaptor = ArgumentCaptor.captor();
        verify(stockRepository).saveAll(stockCaptor.capture());

        assertThat(stockCaptor.getValue()).allSatisfy(stock -> {
            // load-test.js 가 amount 를 5000 으로 하드코딩하므로 단가가 다르면 PAYMENT_AMOUNT_MISMATCH 로 전량 실패한다.
            assertThat(stock.getPrice()).isEqualByComparingTo("5000");
            assertThat(stock.getAvailableQuantity()).isGreaterThanOrEqualTo(5_000);
        });
    }

    @Test
    @DisplayName("이미 시드된 DB에 재기동하면 중복 적재하지 않는다")
    void 재기동_멱등성() {
        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.of(mock(Member.class)));
        when(whiskeyRepository.count()).thenReturn(25L);
        when(stockRepository.count()).thenReturn(25L);
        when(whiskeyRepository.findAll()).thenReturn(whiskeysWithIds(25));
        when(stockRepository.findAll()).thenReturn(stocksWithIds(25));

        seeder().run(null);

        verify(memberRepository, never()).save(any());
        verify(whiskeyRepository, never()).saveAll(any());
        verify(stockRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("loadtest 프로파일에서만 빈이 등록된다 (local/prod 부팅 시 미동작)")
    void 프로파일_가드() {
        contextRunner("loadtest").run(context -> assertThat(context).hasSingleBean(LoadTestDataSeeder.class));
        contextRunner("local").run(context -> assertThat(context).doesNotHaveBean(LoadTestDataSeeder.class));
        contextRunner("prod").run(context -> assertThat(context).doesNotHaveBean(LoadTestDataSeeder.class));
    }

    private void givenEmptyDatabase() {
        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(whiskeyRepository.count()).thenReturn(0L);
        when(stockRepository.count()).thenReturn(0L);
        when(whiskeyRepository.findAll()).thenReturn(whiskeysWithIds(25));
        when(stockRepository.findAll()).thenReturn(stocksWithIds(25));
    }

    private LoadTestDataSeeder seeder() {
        LoadTestDataSeeder seeder = new LoadTestDataSeeder(memberRepository, whiskeyRepository, stockRepository, passwordEncoder);
        ReflectionTestUtils.setField(seeder, "stockQuantity", STOCK_QUANTITY);
        return seeder;
    }

    private ApplicationContextRunner contextRunner(String profile) {
        return new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
            .withBean(MemberRepository.class, () -> memberRepository)
            .withBean(WhiskeyRepository.class, () -> whiskeyRepository)
            .withBean(StockRepository.class, () -> stockRepository)
            .withBean(PasswordEncoder.class, () -> passwordEncoder)
            .withUserConfiguration(LoadTestDataSeeder.class);
    }

    private List<Whiskey> whiskeysWithIds(int count) {
        return IntStream.rangeClosed(1, count)
            .mapToObj(i -> {
                Whiskey whiskey = Whiskey.builder()
                    .distillery("distillery" + i)
                    .name("whiskey" + i)
                    .country("스코틀랜드")
                    .age(12)
                    .volume(700)
                    .maltType(MaltType.SINGLE_MALT)
                    .abv(40.0)
                    .build();
                ReflectionTestUtils.setField(whiskey, "id", (long) i);
                return whiskey;
            })
            .toList();
    }

    private List<Stock> stocksWithIds(int count) {
        return IntStream.rangeClosed(1, count)
            .mapToObj(i -> {
                Stock stock = Stock.of((long) i, STOCK_QUANTITY, new BigDecimal("5000"));
                ReflectionTestUtils.setField(stock, "id", (long) i);
                return stock;
            })
            .toList();
    }
}
