import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Rate, Trend } from 'k6/metrics';

// Custom Metrics
const purchaseSuccessRate = new Rate('purchase_success_rate');
const purchaseDuration = new Trend('purchase_duration');

// 재고 예약 낙관락 재시도가 소진되어 409(STOCK_RESERVATION_CONFLICT)로 떨어진 비율.
// 고경합 시나리오(#64/#65)의 관측 지표이며, baseline 을 뜬 뒤에 threshold 를 건다.
const stockConflictRate = new Rate('stock_conflict_rate');

// 409 는 재시도 소진 후의 정상 응답이므로 http_req_failed 에 섞이면 안 된다.
// (섞이면 고경합을 제대로 재현할수록 threshold 가 깨진다)
const ORDER_EXPECTED_STATUSES = http.expectedStatuses(200, 409);

// VU 산정 근거 (리틀의 법칙: 필요 VU = 도착률 × 이터레이션 소요시간)
// arrival-rate executor 는 도착률로 부하를 고정하므로 이터레이션 안에 think time(sleep)을 두지 않는다.
// sleep 을 두면 VU 만 점유해서, 앱이 느려질 때 k6 가 부하를 줄여버리고 그 사실이
// 지연 상승이 아니라 dropped_iterations 로 숨는다.
//
//   browse   피크 21/s : 정상(GET 1회 ~50ms) 1.1 VU / 응답 1s 21 VU / 응답 3s 63 VU
//   purchase 피크  9/s : 정상(POST 3회 ~240ms) 2.2 VU / 응답 1s 9 VU / 응답 3s 27 VU
//
// preAllocatedVUs 는 정상 구간을 여유 있게 덮어 테스트 중 VU 지연 할당이 없도록,
// maxVUs 는 지연이 크게 튀어도 이터레이션을 흘리지 않도록 넉넉히 잡는다.
export const options = {
  scenarios: {
    browse: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 30,  // 응답 1.4s 까지 커버
      maxVUs: 100,          // 응답 4.8s 까지 커버
      stages: [
        { target: 7, duration: '1m' },   // Warm-up
        { target: 14, duration: '3m' },  // Sustained
        { target: 21, duration: '2m' },  // Peak
        { target: 0, duration: '1m' },   // Cool-down
      ],
      exec: 'browse',
    },
    purchase: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 20,  // 응답 2.2s 까지 커버
      maxVUs: 60,           // 응답 6.7s 까지 커버
      stages: [
        { target: 3, duration: '1m' },
        { target: 6, duration: '3m' },
        { target: 9, duration: '2m' },
        { target: 0, duration: '1m' },
      ],
      exec: 'purchase',
    },
  },
  thresholds: {
    // 시나리오별로 분리한다. GET 1회와 POST 3회 흐름을 한 p95 로 뭉치면 어느 쪽이 느려졌는지 안 보인다.
    // scenario 태그를 붙이면 setup() 의 로그인 요청(BCrypt 라 앱에서 제일 느리다)도 자동으로 제외된다.
    'http_req_duration{scenario:browse}': ['p(95)<300'],
    'http_req_duration{scenario:purchase}': ['p(95)<800'],
    'http_req_failed{scenario:browse}': ['rate<0.01'],
    'http_req_failed{scenario:purchase}': ['rate<0.01'],
    // VU 고갈로 부하를 못 넣고도 PASS 로 뜨는 것을 막는다. 1건이라도 흘리면 측정값을 믿을 수 없다.
    dropped_iterations: ['count<1'],
    purchase_success_rate: ['rate>0.95'],
  },
};

// 실행 예시:
//   로컬     : k6 run tests/k6/load-test.js
//   원격     : k6 run -e BASE_URL=https://api.example.com tests/k6/load-test.js
//   k6 Cloud : k6 cloud run -e BASE_URL=https://api.example.com tests/k6/load-test.js
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const WHISKEY_IDS = Array.from({ length: 25 }, (_, i) => i + 1);
const STOCK_IDS = [1, 2, 3, 4, 5];

const FILTER_COMBINATIONS = [
  { maltType: 'SINGLE_MALT' },
  { country: '스코틀랜드' },
  { maltType: 'BOURBON' },
  { maltType: 'BLENDED', country: '스코틀랜드' },
  {},
];

function buildQueryString(params) {
  return Object.entries(params)
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
      .join('&');
}

// 계정은 loadtest 프로필 부팅 시 LoadTestDataSeeder 가 적재한다 (이슈 #61).
// 시드가 없으면 여기서 즉시 중단시켜, VU 실행 중 session.token 접근 에러로 뒤늦게 터지는 것을 막는다.
export function setup() {
  const accounts = Array.from({ length: 9 }, (_, i) => {
    const num = String(i + 1);
    return {
      email: `tester${num}${num}@example.com`,
      password: `tester${num}${num}@@`,
    };
  });

  const sessions = accounts.map(account => {
    const res = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify(account),
        { headers: { 'Content-Type': 'application/json' } }
    );

    if (res.status !== 200) {
      throw new Error(
          `setup 로그인 실패 - email: ${account.email}, status: ${res.status}. ` +
          `SPRING_PROFILES_ACTIVE=loadtest 로 기동해 시드 데이터가 적재됐는지 확인할 것.`
      );
    }

    const body = JSON.parse(res.body);
    return {
      token: body.data.accessToken,
      memberId: body.data.memberInfo.id,
    };
  });

  return { sessions };
}

// sleep 을 걷어내면 정상 구간에서 동시 VU 가 1~2개까지 떨어져서,
// __VU 기준으로 세션을 고르면 계정 1~2개만 쓰이게 된다. 이터레이션 기준으로 돌려 고르게 분산시킨다.
function pickSession(sessions) {
  return sessions[exec.scenario.iterationInTest % sessions.length];
}

export function browse({ sessions }) {
  const session = pickSession(sessions);
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${session.token}`,
  };

  if (Math.random() < 0.5) {
    const whiskeyId = WHISKEY_IDS[Math.floor(Math.random() * WHISKEY_IDS.length)];
    const res = http.get(`${BASE_URL}/api/whiskey/${whiskeyId}`, { headers });
    check(res, { '단건 조회 성공': r => r.status === 200 });
  } else {
    const filter = FILTER_COMBINATIONS[Math.floor(Math.random() * FILTER_COMBINATIONS.length)];
    const query = buildQueryString({ size: 10, ...filter });
    const res = http.get(`${BASE_URL}/api/whiskey?${query}`, { headers });
    check(res, { '목록 조회 성공': r => r.status === 200 });
  }
}

export function purchase({ sessions }) {
  const session = pickSession(sessions);
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${session.token}`,
  };

  const startTime = Date.now();
  const stockId = STOCK_IDS[Math.floor(Math.random() * STOCK_IDS.length)];

  // Step 1. 주문 생성
  const orderRes = http.post(
      `${BASE_URL}/api/order?memberId=${session.memberId}`,
      JSON.stringify({ items: [{ stockId, quantity: 1 }] }),
      { headers, responseCallback: ORDER_EXPECTED_STATUSES }
  );

  stockConflictRate.add(orderRes.status === 409);

  if (!check(orderRes, { '주문 생성 성공': r => r.status === 200 })) {
    purchaseSuccessRate.add(false);
    return;
  }

  const orderId = JSON.parse(orderRes.body).data.orderId;

  // Step 2. 결제 준비
  const prepareRes = http.post(
      `${BASE_URL}/api/payments/prepare?memberId=${session.memberId}`,
      JSON.stringify({ orderId, amount: 5000 }),
      { headers }
  );

  if (!check(prepareRes, { '결제 준비 성공': r => r.status === 200 })) {
    purchaseSuccessRate.add(false);
    return;
  }

  const { orderId: paymentOrderId } = JSON.parse(prepareRes.body).data;

  // Step 3. 결제 승인
  const confirmRes = http.post(
      `${BASE_URL}/api/payments/confirm?memberId=${session.memberId}`,
      JSON.stringify({
        paymentKey: `mock_payment_key_${paymentOrderId}`,
        orderId: paymentOrderId,
        amount: 5000,
      }),
      { headers }
  );

  const success = check(confirmRes, { '결제 승인 성공': r => r.status === 200 });
  purchaseSuccessRate.add(success);
  purchaseDuration.add(Date.now() - startTime);
}
