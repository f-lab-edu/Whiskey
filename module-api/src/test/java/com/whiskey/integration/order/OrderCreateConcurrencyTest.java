

package com.whiskey.integration.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.whiskey.domain.order.dto.OrderCommand;
import com.whiskey.domain.order.dto.OrderCommand.OrderItem;
import com.whiskey.domain.order.service.OrderService;
import com.whiskey.domain.stock.Stock;
import com.whiskey.domain.stock.repository.StockRepository;
import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.enums.MaltType;
import com.whiskey.domain.whiskey.repository.WhiskeyRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 이슈 #64 검증 테스트.
 *
 * <p>동일 재고(핫 스톡)에 동시 주문이 몰릴 때, {@code OrderService.createOrder}에 건
 * {@code @Retryable}이 실제로 동작하여 재고 정합성이 유지되는지 검증한다.</p>
 *
 * <p>이 스키마에서 동시성 충돌은 낙관락 버전 불일치
 * ({@code ObjectOptimisticLockingFailureException})뿐 아니라, {@code stock_reservation}이
 * {@code stocks}를 FK 참조하며 INSERT(공유락)+UPDATE(배타락)가 겹쳐 발생하는
 * InnoDB 데드락/락 대기({@code CannotAcquireLockException})로도 표면화된다.
 * 재시도는 두 경우의 공통 상위인 {@code ConcurrencyFailureException}을 대상으로 한다.</p>
 *
 * <p>재시도가 없던 기존 코드에서는 충돌한 주문이 그대로 실패해
 * {@code failures}가 비지 않고 최종 재고도 0이 되지 않는다. 재시도가 동작하면
 * 두 주문 모두 성공하고 재고는 정확히 0이 된다.</p>
 *
 * <p>동시 스레드 수는 2로 둔다. 충돌이 나면 패한 트랜잭션 하나만 재시도되고,
 * 그 시점엔 경쟁자가 이미 커밋을 마쳤으므로 재시도 1회로 반드시 성공한다
 * → {@code maxAttempts=3} 한도 안에서 결정적으로 재시도 동작을 증명한다.
 * (3 이상의 고경합 N-way 파일업 해소는 이 테스트의 범위가 아니며, 지터/분산락(#65) 영역)</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderCreateConcurrencyTest {

    private static final int CONCURRENCY = 2;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private WhiskeyRepository whiskeyRepository;

    @Test
    @DisplayName("동일 재고에 동시 주문이 몰려도 낙관락 재시도로 재고 정합성이 유지된다")
    void 동시주문_낙관락재시도_재고정합성_유지() throws InterruptedException {
        // given : 재고 CONCURRENCY개짜리 핫 스톡 1건
        Whiskey whiskey = whiskeyRepository.save(Whiskey.builder()
            .distillery("Test Distillery")
            .name("Concurrency Test Whiskey")
            .country("Scotland")
            .age(12)
            .volume(700)
            .maltType(MaltType.SINGLE_MALT)
            .abv(40.0)
            .description("issue #64 concurrency verification")
            .build());

        Stock stock = stockRepository.save(
            Stock.of(whiskey.getId(), CONCURRENCY, new BigDecimal("50000")));
        Long stockId = stock.getId();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(CONCURRENCY);
        AtomicInteger successCount = new AtomicInteger();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        // when : 동일 stockId에 각 1개씩 동시 주문
        for (int i = 0; i < CONCURRENCY; i++) {
            long memberId = i + 1L;
            executor.submit(() -> {
                try {
                    startGate.await();
                    OrderCommand command = new OrderCommand(memberId,
                        List.of(new OrderItem(stockId, 1)));
                    orderService.createOrder(command);
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();                      // 동시에 출발
        boolean finished = doneGate.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        // then : 전부 성공하고, 재고는 정확히 0 (lost update 없음)
        assertThat(finished).as("모든 주문 스레드가 30초 내 종료").isTrue();
        assertThat(failures).as("낙관락 재시도로 실패 없이 처리되어야 함").isEmpty();
        assertThat(successCount.get()).isEqualTo(CONCURRENCY);

        Stock reloaded = stockRepository.findById(stockId).orElseThrow();
        assertThat(reloaded.getAvailableQuantity()).isZero();
    }
}
