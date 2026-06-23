import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom Metrics
const purchaseSuccessRate = new Rate('purchase_success_rate');
const purchaseDuration = new Trend('purchase_duration');

export const options = {
  scenarios: {
    browse: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 20,
      maxVUs: 50,
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
      preAllocatedVUs: 10,
      maxVUs: 20,
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
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
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
      console.error(`로그인 실패 - email: ${account.email}`);
      return null;
    }

    const body = JSON.parse(res.body);
    return {
      token: body.data.accessToken,
      memberId: body.data.memberInfo.id,
    };
  });

  return { sessions };
}

export function browse({ sessions }) {
  const session = sessions[(__VU - 1) % sessions.length];
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

  sleep(Math.random() * 2 + 1);
}

export function purchase({ sessions }) {
  const session = sessions[(__VU - 1) % sessions.length];
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
      { headers }
  );

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

  sleep(Math.random() * 2 + 1);
}
