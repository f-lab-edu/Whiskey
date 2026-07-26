import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Rate, Trend } from 'k6/metrics';
import { setup } from './load-test.js';

// 단일 재고에 동시 주문을 몰아 낙관락 경합을 재현한다. (#64)
//
// load-test.js 는 재고 5개에 분산해서 피크에도 재고당 1.8 req/s 밖에 안 되고, 그 부하로는
// 버전 충돌이 거의 나지 않아 OrderService 의 @Retryable 이 실제로 도는지 확인할 수 없다.
// 이 스크립트는 재고 하나만 때려서 충돌을 강제한다.
//
// 실행 예시:
//   k6 run k6/hot-stock-test.js
//   k6 run -e HOT_STOCK_ID=3 -e BURST_VUS=100 k6/hot-stock-test.js
//
// 재시도가 실제로 몇 번 돌았는지는 앱 로그로 본다. RETRY_LOG_LEVEL=DEBUG 로 기동하면
// RetryTemplate 이 시도마다 "Retry: count=N" 을 남긴다 (count=0 이 첫 시도).
//
// 목적은 "재시도가 도는지"의 재현이다. 락 전략별 처리량/지연 비교 하네스는 여기 두지 않았다.
// 그건 실행 간 상태 초기화(주문·예약 정리, 재고 리셋)와 만료 스케줄러 격리가 선행돼야 하고,
// 비교 대상을 구현하면서 확정하는 게 맞다. 측정해 둔 용량 곡선은 이슈에 남겼다.

// 로그인은 load-test.js 와 동일하다. 시드가 없으면 setup 에서 즉시 중단된다.
export { setup };

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const HOT_STOCK_ID = Number(__ENV.HOT_STOCK_ID || 1);
const BURST_VUS = Number(__ENV.BURST_VUS || 50);

// 주문 생성(@Retryable 보호 구간)에서 재시도를 소진해 409 로 떨어진 비율.
const orderConflictRate = new Rate('order_conflict_rate');
const orderSuccessRate = new Rate('order_success_rate');
// 재시도 백오프(100ms × 2^n, 지터)가 지연에 그대로 얹히므로 여기서 드러난다.
const orderDuration = new Trend('order_duration', true);

// 결제 승인은 주문 확정에서 같은 Stock 행을 갱신하는데 @Retryable 이 없다.
// 여기서 실패하면 보상 트랜잭션 → DLQ 경로로 빠진다. 주문 생성과 대비해서 보려고 따로 잡는다.
const confirmSuccessRate = new Rate('confirm_success_rate');
const purchaseSuccessRate = new Rate('purchase_success_rate');

// 409(STOCK_RESERVATION_CONFLICT)는 재시도 소진 후의 정상 응답이므로 http_req_failed 와 섞지 않는다.
const ORDER_EXPECTED_STATUSES = http.expectedStatuses(200, 409);

export const options = {
  scenarios: {
    // 모든 VU 가 동시에 출발해 같은 재고 행을 한순간에 때린다. 순간 동시성이 최대가 된다.
    burst: {
      executor: 'shared-iterations',
      vus: BURST_VUS,
      iterations: BURST_VUS,
      maxDuration: '1m',
      exec: 'hotStockPurchase',
    },
  },
  // 경합을 일으키는 게 목적이므로 충돌률/지연에 임계값을 걸지 않는다. 전부 관측치다.
  thresholds: {},
};

export function hotStockPurchase({ sessions }) {
  const session = sessions[exec.scenario.iterationInTest % sessions.length];
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${session.token}`,
  };

  // Step 1. 주문 생성 - 재고 예약에서 버전 충돌이 나고, @Retryable 이 최대 3회까지 재실행한다.
  const orderStart = Date.now();
  const orderRes = http.post(
      `${BASE_URL}/api/order`,
      JSON.stringify({ items: [{ stockId: HOT_STOCK_ID, quantity: 1 }] }),
      { headers, responseCallback: ORDER_EXPECTED_STATUSES }
  );
  orderDuration.add(Date.now() - orderStart);

  orderConflictRate.add(orderRes.status === 409);
  orderSuccessRate.add(orderRes.status === 200);

  if (orderRes.status !== 200) {
    // 409 는 재시도 소진(설계된 동작). 그 밖의 상태 코드는 진짜 문제다.
    check(orderRes, { '주문 생성 - 409 외의 실패 없음': r => r.status === 409 });
    purchaseSuccessRate.add(false);
    return;
  }

  const orderId = JSON.parse(orderRes.body).data.orderId;

  // Step 2. 결제 준비
  const prepareRes = http.post(
      `${BASE_URL}/api/payments/prepare`,
      JSON.stringify({ orderId, amount: 5000 }),
      { headers }
  );

  if (!check(prepareRes, { '결제 준비 성공': r => r.status === 200 })) {
    purchaseSuccessRate.add(false);
    return;
  }

  const { orderId: paymentOrderId } = JSON.parse(prepareRes.body).data;

  // Step 3. 결제 승인 - 주문 확정이 같은 Stock 행을 갱신한다. 여기엔 재시도가 없다.
  const confirmRes = http.post(
      `${BASE_URL}/api/payments/confirm`,
      JSON.stringify({
        paymentKey: `mock_payment_key_${paymentOrderId}`,
        orderId: paymentOrderId,
        amount: 5000,
      }),
      { headers }
  );

  const confirmed = check(confirmRes, { '결제 승인 성공': r => r.status === 200 });
  confirmSuccessRate.add(confirmed);
  purchaseSuccessRate.add(confirmed);
}
