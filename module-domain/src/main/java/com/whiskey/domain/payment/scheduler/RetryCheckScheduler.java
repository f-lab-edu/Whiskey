package com.whiskey.domain.payment.scheduler;

import com.whiskey.domain.payment.dto.CompensatePaymentInfo;
import com.whiskey.domain.payment.dto.PaymentCompensationRequest;
import com.whiskey.domain.payment.service.PaymentCompensationQueue;
import com.whiskey.domain.payment.service.PaymentCompensationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryCheckScheduler {

    private final PaymentCompensationQueue paymentCompensationQueue;
    private final PaymentCompensationService paymentCompensationService;

    private static final int MAX_RETRY_COUNT = 5;
    private static final long RETRY_INTERVAL_MINUTES = 5;

    @Scheduled(fixedDelay = 60000)
    public void retryCheck() {
        for(CompensatePaymentInfo compensatePaymentInfo : paymentCompensationQueue.findDue(LocalDateTime.now())) {
            // 한 건이 터져도 남은 건은 처리해야 하므로 항목 단위로 격리한다.
            // (스케줄 메서드가 예외를 올리면 그 틱의 나머지 항목이 통째로 밀린다)
            try {
                retryOne(compensatePaymentInfo);
            }
            catch(Exception e) {
                log.error("결제 보상 재시도 처리 실패 - paymentId: {}", compensatePaymentInfo.paymentId(), e);
            }
        }
    }

    private void retryOne(CompensatePaymentInfo compensatePaymentInfo) {
        try {
            paymentCompensationService.cancelPayment(new PaymentCompensationRequest(
                compensatePaymentInfo.paymentId(),
                compensatePaymentInfo.orderId(),
                compensatePaymentInfo.paymentOrderId(),
                compensatePaymentInfo.paymentKey()
            ));
        }
        catch(Exception e) {
            reschedule(compensatePaymentInfo, e);
            return;
        }

        // 성공한 항목은 큐에서 지운다. 남겨두면 매 틱마다 같은 보상을 다시 시도한다.
        paymentCompensationQueue.remove(compensatePaymentInfo.paymentId());
        log.info("결제 보상 트랜잭션 재시도 성공 - paymentId: {}", compensatePaymentInfo.paymentId());
    }

    private void reschedule(CompensatePaymentInfo compensatePaymentInfo, Exception cause) {
        long attempts = paymentCompensationQueue.incrementRetryCount(compensatePaymentInfo.paymentId());

        if(attempts >= MAX_RETRY_COUNT) {
            // 재시도 대상으로 남겨두면 무한 재시도가 된다. payload 는 남으므로 조회는 계속 가능하다.
            // TODO: 전용 테이블로 이관해 관리자가 화면에서 처리할 수 있게 한다.
            paymentCompensationQueue.moveToDeadLetter(compensatePaymentInfo.paymentId());

            log.error("결제 보상 트랜잭션 재시도 {}회 소진 - 수동 처리 필요. paymentId: {}, paymentKey: {}, reason: {}",
                attempts, compensatePaymentInfo.paymentId(), compensatePaymentInfo.paymentKey(),
                compensatePaymentInfo.reason(), cause);
            return;
        }

        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(attempts * RETRY_INTERVAL_MINUTES);
        paymentCompensationQueue.reschedule(compensatePaymentInfo.paymentId(), nextRetryAt);

        log.warn("결제 보상 트랜잭션 실패({}/{}) - 다음 재시도 : {}", attempts, MAX_RETRY_COUNT, nextRetryAt, cause);
    }
}
