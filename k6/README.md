# k6 부하 테스트

| 파일 | 목적 |
|---|---|
| `load-test.js` | 혼합 트래픽 baseline 측정 (조회 + 구매) |
| `hot-stock-test.js` | 단일 핫 재고 경합 재현 (낙관락 재시도 검증) |

---

## 사전 준비

### 1. 테스트 인프라

```bash
docker compose -f docker/docker-compose.test.yml up -d   # mysql-test:3307, redis-test:6380
docker exec mysql-test mysql -uroot -proot1234 -e "CREATE DATABASE IF NOT EXISTS whiskey_loadtest"
```

### 2. 앱 기동 (`loadtest` 프로필 필수)

```bash
DB_URL="jdbc:mysql://127.0.0.1:3307/whiskey_loadtest?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8" \
DB_USERNAME=root DB_PASSWORD=root1234 \
REDIS_HOST=127.0.0.1 REDIS_PORT=6380 \
TOSS_SECRET_KEY=dummy TOSS_CLIENT_KEY=dummy \
SPRING_PROFILES_ACTIVE=loadtest \
./gradlew :module-api:bootRun
```

`loadtest` 프로필에서는 `MockPaymentClient`가 활성화되므로 Toss 키는 더미값이면 된다.

### 3. 시드 데이터

`loadtest` 프로필로 부팅하면 `LoadTestDataSeeder`가 멱등하게 적재한다. 별도 작업 없음.

| 대상 | 수량 | 비고 |
|---|---|---|
| 회원 | 9 | `tester11@example.com` ~ `tester99@example.com` / 비밀번호 `tester11@@` 형식 |
| 위스키 | 25 | id 1~25 |
| 재고 | 25 | id 1~25, 수량 10,000, **단가 전건 5,000** |

**스크립트가 ID를 하드코딩한다.** 위스키 id 1\~25, 재고 id 1\~5를 가정하고, 결제 금액도 5,000으로
고정돼 있다(단가가 다르면 `PAYMENT_AMOUNT_MISMATCH`로 전량 실패). 비어있지 않은 DB에서
AUTO_INCREMENT가 밀려 전제가 깨지면 시더가 부팅 로그에 WARN을 남긴다. 그때는 전용 DB를
비우고(TRUNCATE) 재기동한다.

시드가 없으면 `setup()`에서 즉시 중단된다. VU 실행 중에 `session.token` 접근 에러로 뒤늦게
터지는 것을 막기 위한 의도된 동작이다.

---

## `load-test.js` — baseline 측정

```bash
k6 run k6/load-test.js
k6 run -e BASE_URL=https://api.example.com k6/load-test.js
```

`browse`(조회)와 `purchase`(주문 → 결제 준비 → 결제 승인) 두 시나리오를 동시에 램프업한다.
피크 합계 30 TPS(browse 21 + purchase 9), 총 7분(warm-up 1 / sustained 3 / peak 2 / cool-down 1).

> 이 30 TPS와 아래 threshold 값은 **목표치에서 역산한 것이 아니다.** 요청 형태(GET 1회 vs POST 3회)를
> 반영해 나눈 작업 가설이다. 합격 기준으로 쓰려면 목표 TPS와 지연 기준을 현재 인프라 기준으로
> 다시 정의해야 한다. (미해결 항목 참고)

### VU 산정 근거

`ramping-arrival-rate`는 **도착률로 부하를 고정**한다. 필요한 VU 수는 리틀의 법칙으로 나온다.

```
필요 VU = 도착률 × 이터레이션 소요시간
```

이터레이션 안에 `sleep`(think time)을 두지 않는다. 도착률 방식에서는 think time이 이미 도착률에
반영돼 있어 sleep은 VU만 점유하고, **앱이 느려질 때 k6가 부하를 줄여버려 성능 저하가 지연 상승이
아니라 `dropped_iterations`로 숨는다.**

| 시나리오 | 피크 | 정상 지연 시 | 응답 1s 시 | 응답 3s 시 | preAllocatedVUs / maxVUs |
|---|---|---|---|---|---|
| browse | 21/s | 1.1 | 21 | 63 | 30 / 100 |
| purchase | 9/s | 2.2 | 9 | 27 | 20 / 60 |

`preAllocatedVUs`는 정상 구간을 덮어 테스트 중 VU 지연 할당이 없게, `maxVUs`는 지연이 튀어도
이터레이션을 흘리지 않게 잡았다.

세션은 `exec.scenario.iterationInTest` 기준 라운드로빈으로 고른다. sleep을 걷어내면 정상 구간
동시 VU가 1~2개까지 떨어져서, `__VU` 기준으로 고르면 계정 1~2개만 쓰이게 된다.

### threshold 설계

```js
'http_req_duration{scenario:browse}':   ['p(95)<300'],
'http_req_duration{scenario:purchase}': ['p(95)<800'],
'http_req_failed{scenario:browse}':     ['rate<0.01'],
'http_req_failed{scenario:purchase}':   ['rate<0.01'],
dropped_iterations:                     ['count<1'],
purchase_success_rate:                  ['rate>0.95'],
```

+ **시나리오별로 분리한다.** GET 1회와 POST 3회 흐름을 한 p95로 뭉치면 어느 쪽이 느려졌는지
  안 보인다. `scenario` 태그를 쓰면 `setup()`의 로그인 요청(BCrypt라 앱에서 가장 느리다)도
  자동으로 제외된다.
+ **`dropped_iterations`는 0이어야 한다.** VU 고갈로 부하를 못 넣고도 PASS로 뜨는 것을 막는
  안전장치다. 1건이라도 흘리면 그 실행의 측정값은 신뢰할 수 없다.
+ **409는 실패가 아니다.** 주문 생성 요청에 `http.expectedStatuses(200, 409)`를 걸어
  `STOCK_RESERVATION_CONFLICT`를 `http_req_failed`에서 제외하고, `stock_conflict_rate`로 따로
  관측한다. 섞으면 고경합을 제대로 재현할수록 threshold가 깨진다.

### 커스텀 메트릭

| 메트릭 | 의미 |
|---|---|
| `purchase_success_rate` | 주문 → 결제 승인 3단계 전체 성공률 |
| `purchase_duration` | 구매 흐름 전체 소요시간 (성공한 흐름만 기록 — 아래 미해결 항목 참고) |
| `stock_conflict_rate` | 주문 생성이 409로 떨어진 비율 (낙관락 재시도 소진) |

---

## `hot-stock-test.js` — 경합 재현

```bash
k6 run k6/hot-stock-test.js
k6 run -e HOT_STOCK_ID=3 -e BURST_VUS=100 k6/hot-stock-test.js
```

`load-test.js`는 재고 5개에 분산해서 피크에도 **재고당 1.8 req/s**밖에 안 된다. 그 부하로는 버전
충돌이 거의 나지 않아 `OrderService`의 `@Retryable`이 실제로 도는지 확인할 수 없다. 이 스크립트는
재고 하나만 때려 충돌을 강제한다.

`shared-iterations`로 모든 VU를 동시에 출발시켜 순간 동시성을 최대로 만든다. 충돌률·지연에
threshold를 걸지 않는다 — 경합을 일으키는 게 목적이라 전부 관측치다.

### 재시도 관측 방법

`OrderService`는 재시도 **소진 시점**(`@Recover`)만 로그를 남긴다. 성공한 재시도는 로그가 없다.
`RETRY_LOG_LEVEL=DEBUG`로 기동하면 Spring `RetryTemplate`이 시도마다 `Retry: count=N`을 남긴다
(`count=0`이 첫 시도).

```bash
grep -aoE "Retry: count=[0-9]+" app.log | sort | uniq -c
```

baseline 측정 시에는 주문 1건마다 로그가 찍혀 지연에 섞이므로 기본값 `INFO`로 둔다.

### 실측 결과 (로컬, 재고 1개 / 50 VU 동시)

주문 생성 (`@Retryable maxAttempts=3`, backoff 100ms × 2 지터)

| 시도 | 실행 건수 | 그 시도에서 성공 |
|---|---|---|
| 1회차 | 50 | 17 |
| 2회차 | 33 | 0 |
| 3회차 | 33 | 2 |
| 소진 → 409 | 31 | — |

**재시도 2회를 추가해서 건진 것이 50건 중 2건이다.** 지터를 넣어도 재시도가 서로 다시 충돌한다.
낙관락은 경합 시 대기가 아니라 폐기이므로, 동시 쓰기 N개면 시도당 성공 확률이 대략 1/N로 떨어진다.
재시도 횟수를 늘려도 구조적으로 따라잡지 못한다.

실패 원인 분포도 중요하다.

```
InnoDB 데드락             80건
낙관락 버전충돌(@Version)  34건
```

`@Retryable`이 둘의 공통 상위인 `ConcurrencyFailureException`을 잡기 때문에 가려져 있었다.
데드락은 락 획득 순서 문제이므로 비관락으로 바꾸기만 해도 남는다.

결제 승인(주문 확정)은 같은 재고 행을 갱신하는데 재시도가 없다. 성공률 21%, 최종 구매 성공률 8%.

지속 부하 용량 곡선 (별도 측정)

| 피크 도착률 | 주문 충돌률 | 주문 지연(중앙) | dropped_iterations |
|---|---|---|---|
| 4/s | 40.6% | 1.37s | 1 |
| 8/s | 56.7% | 2.74s | 32 |
| 30/s | 60.8% | 15.4s | 734 (포화, 측정 불가) |

**락 전략별 처리량/지연 비교 하네스는 이 디렉토리에 없다.** 아래 재현성 제약을 먼저 해소해야
의미 있는 비교가 나오고, 비교 대상(DB 비관락 / 분산락 / 경합 감소)을 구현하면서 확정하는 게 맞다.

---

## 측정 시 주의

### 실행 간 상태가 다음 실행에 영향을 준다

이전 실행이 남긴 PENDING 주문이 만료 스케줄러에 의해 처리되면서 **같은 재고 행을 계속 갱신한다.**
실측으로 앞선 부하가 있을 때 주문 지연 중앙값이 2.74s → 9.21s로 벌어졌다. 연속 측정 전에는
PENDING이 빠질 때까지 기다린다.

```bash
docker exec mysql-test mysql -uroot -proot1234 -N \
  -e "SELECT COUNT(*) FROM whiskey_loadtest.orders WHERE order_status='PENDING'"
```

### 스케줄러가 측정 중에 계속 개입한다

주문 만료(`ExpireCheckScheduler`)와 결제 재시도(`RetryCheckScheduler`)가 1분 주기로 돌면서 주문·재고
행을 갱신한다. 특히 `loadtest` 프로필의 `order.reservation.expire-time`은 **1분**이고 운영 기본값은
**10분**이다. 반복 측정 시 재고를 회전시키려는 설정이지만, 그만큼 만료 스케줄러가 측정 구간에
개입하는 빈도가 운영보다 훨씬 높다.

스케줄러를 포함해서 측정할지(운영과 같은 조건) 격리할지는 정해야 한다. 포함한다면 `expire-time`을
운영과 맞춰야 조건이 같아지고, 그러면 재고가 회전하지 않아 실행마다 DB를 비워야 한다. 지금은 어느
쪽도 아닌 상태다.

### 커넥션 풀이 명시돼 있지 않다

`application-loadtest.yml`에 풀 설정이 없어 HikariCP 기본값 **10**에 의존한다. Tomcat 스레드
기본값(200) 대비 20:1이라 포화 시 대기가 풀에서 증폭된다. 부하 중
`Connection is not available` 로그가 보이면 앱 병목이 아니라 이 제약일 수 있다.

동시 트랜잭션이 10개로 묶인다는 뜻이므로, 경합 실험 결과(핫 재고 충돌률 등)도 이 값에 좌우된다.
풀을 키우면 낙관락은 더 나빠지고 비관락은 대기가 늘어난다. **락 전략을 비교하려면 풀 크기를 먼저
명시적으로 고정해야 한다.**

### 재고 소진

시드 수량은 재고당 10,000이다. 확정(결제 완료)된 주문은 재고를 돌려주지 않으므로 반복 측정하면
줄어든다. 남은 수량을 확인하고, 필요하면 전용 DB를 비우고 재기동한다.

```bash
docker exec mysql-test mysql -uroot -proot1234 \
  -e "SELECT id, available_quantity FROM whiskey_loadtest.stocks WHERE id <= 5"
```

수량은 `application-loadtest.yml`의 `loadtest.seed.stock-quantity`로 조정한다.

---

## 미해결 항목

| 항목 | 내용 |
|---|---|
| 계정 수 | 9개뿐이라 동시 VU 최대 160개가 세션을 공유한다. `order.member_id` 카디널리티가 9로 고정돼, "내 주문 조회" 같은 시나리오를 추가하면 측정이 무의미해진다. 100~200개로 늘리려면 시더의 이메일 규칙(`tester${n}${n}`은 9까지만 유효)과 `setup()`의 순차 로그인(k6 기본 `setupTimeout` 60초)을 함께 고쳐야 한다. |
| 데이터 규모 | `browse`가 트래픽의 70%인데 대상 테이블이 25행이다. `country LIKE '%…%'`가 25행 풀스캔이라 인덱스 유무가 결과에 나타나지 않고, 전 데이터가 버퍼풀에 상주해 측정값이 최선의 경우다. |
| 워밍업 포함 | `load-test.js`의 1분 warm-up 구간이 metrics에 그대로 들어간다. JVM JIT 컴파일 구간이 p95를 끌어올린다. |
| 토큰 만료 | `access-token-validity`가 10분이고 테스트가 7분이라 여유가 3분뿐이다. 스테이지를 늘리면 전량 401이 되고 갱신 로직이 없다. |
| `purchase_duration` 생존 편향 | 중간 실패 시 early return이라 성공한 흐름만 기록된다. 상황이 나빠질수록 수치가 좋아 보인다. |
| 죽은 파라미터 | `load-test.js`가 `?memberId=`를 붙이지만 서버는 무시한다. `@CurrentMemberId`가 SecurityContext(JWT)에서 꺼낸다. |
| 합격 기준 미정의 | 목표 TPS와 지연 기준이 현재 인프라 기준으로 정의돼 있지 않다. 지금 threshold는 작업 가설이라 PASS/FAIL을 품질 판정으로 쓸 수 없다. 측정 환경(로컬 Docker vs 배포 환경)도 함께 확정해야 한다. |
| 풀 크기 고정 | 위 "커넥션 풀이 명시돼 있지 않다" 참고. 락 전략 비교의 전제 조건이다. |
