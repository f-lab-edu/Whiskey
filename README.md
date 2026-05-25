
## 🍸 Whiskey (Whisky + Key)
위스키 애호가를 위한 리뷰 & 구매 플랫폼

+ 다양한 검색 조건으로 위스키 탐색
+ 평점과 리뷰 기반 커뮤니티
+ 안전한 결제 시스템

---

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.0, Spring Security, Spring Batch, Spring AOP, Spring Retry |
| ORM | Spring Data JPA, QueryDSL 5.1.0 |
| DB | MySQL 8.0, Flyway |
| Cache | Redis 7 |
| Auth | JWT (JJWT 0.11.5) |
| Payment | Toss Payments API |
| Infra | Docker, AWS EC2, AWS RDS, AWS ElastiCache |
| CI/CD | GitHub Actions, GHCR, SonarCloud |
| Test | JUnit 5, Mockito, H2, K6 |
| Docs | Springdoc OpenAPI (Swagger) |

---

## 📐 아키텍처 구조

```
whiskey-service/
├── module-api      (Controller, Security Filter, Batch, AOP, Spring Boot Entry Point)
├── module-domain   (Entity, Service, Repository, Event/Listener, Scheduler)
├── module-payment  (PaymentClient 인터페이스, Toss/Mock 구현체)
└── module-common   (API Response, Error Code, JWT, 공통 유틸리티)
```

**모듈 의존 관계**
```
module-api     → module-domain, module-common, module-payment
module-domain  → module-common, module-payment
module-payment → module-common
module-common  → (standalone)
```

+ DB 스키마는 Flyway 마이그레이션(`module-api/src/main/resources/db/migration`, V1~V16)으로 버전 관리
+ 외부 결제 연동은 `PaymentClient` 인터페이스로 추상화 → 운영은 `TossPaymentClient`, 테스트/부하 테스트는 `MockPaymentClient`(`test` 프로파일)

---

## 🚀 실행 방법

```bash
# 1. 로컬 인프라(MySQL 8.0, Redis 7) 기동
docker compose -f docker/docker-compose.yml up -d

# 2. 애플리케이션 실행 (local 프로파일)
./gradlew :module-api:bootRun
```

+ API 문서(Swagger UI) : `http://localhost:8080/swagger-ui.html`
+ 기본 프로파일 : `local` — MySQL `localhost:3306/whiskey`, Redis `localhost:6379`

---

## 📌 주요 기능 & API

### 위스키
| Method | URI | 설명 |
|--------|-----|------|
| `GET` | `/api/whiskey` | 목록 조회 (커서 기반 페이지네이션, 다중 필터) |
| `GET` | `/api/whiskey/{id}` | 단건 조회 (조회 활동 로그 기록) |
| `GET` | `/api/whiskey/{id}/reviews` | 위스키별 리뷰 조회 (최신순 / 평점순 정렬) |
| `POST` | `/api/whiskey` | 등록 (ADMIN) |
| `PUT` | `/api/whiskey/{id}` | 수정 (ADMIN) |
| `DELETE` | `/api/whiskey/{id}` | 삭제 (ADMIN) |

### 회원 & 인증
| Method | URI | 설명 |
|--------|-----|------|
| `POST` | `/api/members` | 회원가입 |
| `GET` | `/api/members/{id}` | 회원 단건 조회 |
| `POST` | `/api/auth/login` | 로그인 (Access + Refresh Token 발급) |
| `POST` | `/api/auth/token/refresh` | Access Token 재발급 |

### 리뷰
| Method | URI | 설명 |
|--------|-----|------|
| `POST` | `/api/reviews` | 리뷰 작성 |
| `PUT` | `/api/reviews/{id}` | 리뷰 수정 |
| `DELETE` | `/api/reviews/{id}` | 리뷰 삭제 |

### 주문 & 결제
| Method | URI | 설명 |
|--------|-----|------|
| `POST` | `/api/order` | 주문 생성 (재고 예약) |
| `PATCH` | `/api/order/{orderId}/cancel` | 주문 취소 |
| `POST` | `/api/payments/prepare` | 결제 준비 (orderId 발급) |
| `POST` | `/api/payments/confirm` | 결제 확정 |

> 대량 리뷰 더미 데이터는 Spring Batch 잡(`POST /api/batch/review-dummy-data`)으로 적재합니다.

---

## 🎯 프로젝트 목표

+ 객체지향 설계원칙을 적용하여 유지보수에 용이한 코드 구현
+ 서버 구축과 CI/CD 자동화를 통한 인프라와 배포 전반에 대한 이해
+ 트레이드오프를 고려한 기술 선택

---

## 🔧 기술적 구현

### 1. Redis Sorted Set 기반 예약 만료 처리

**배경**
- 결제 예약 후 일정 시간(기본 10분) 이내 미결제 시 자동 취소 필요
- 미결제 예약이 재고를 점유하는 문제 방지

**방안 비교**

| | DB 스케줄러 | Quartz | Redis Sorted Set ✅ |
|---|---|---|---|
| 정확도 | 낮음 (최대 N초 지연) | 높음 | 높음 |
| DB 부하 | 있음 (폴링) | 있음 | 없음 |
| 복잡도 | 낮음 | 높음 | 중간 |
| 장애 내성 | 높음 | 높음 | DB 백업으로 보완 |

**최종 결정**
+ 주문 생성 트랜잭션 커밋 후(`AFTER_COMMIT`) 만료 시각을 score로 Redis Sorted Set에 등록
+ `ExpireCheckScheduler`가 1분마다 현재 시각 이하 score를 `rangeByScore`로 일괄 조회·만료 처리
+ 결과 : 폴링으로 인한 상시 DB 부하 없이 안정적인 만료 처리, 평균 지연 < 1분

---

### 2. 이벤트 기반 평점 집계

**배경**
- 리뷰 작성 시 위스키 평점 실시간 업데이트 필요
- 평점은 Redis에 캐싱하여 빠른 조회 지원
- 같은 트랜잭션에서 DB + Redis를 동시에 처리하면 데이터 불일치 발생 가능

**방안 비교**

| | 동기 처리 (@Transactional) | 비동기 (@Async) | @TransactionalEventListener ✅ |
|---|---|---|---|
| 데이터 일관성 | 낮음 (Redis 실패 시 리뷰 롤백) | 낮음 (커밋 전 실행 가능) | 높음 (커밋 후 실행) |
| 리뷰 저장 안정성 | 낮음 | 낮음 | 높음 |

**최종 결정**
+ 리뷰 저장 트랜잭션 커밋 후 `AFTER_COMMIT` 이벤트로 평점 집계 실행 → 리뷰 저장과 분리
+ 평점 합계 / 리뷰 수를 Redis 원자적 연산(`INCR` / `DECR`)으로 갱신해 동시성 문제 회피
+ Redis 일시 장애 대비 `@Retryable`로 최대 5회(1초 백오프) 재시도
+ 결과 : Redis 장애가 리뷰 작성 트랜잭션에 영향을 주지 않음

---

### 3. 결제 API 트랜잭션 분리 + 재시도 + 보상

**배경**
- Toss Payments API 호출이 네트워크 상태에 따라 지연됨
- 주문 트랜잭션 내부에서 외부 API 호출 → DB 커넥션 점유 → 커넥션 풀 고갈 위험

**최종 결정**
+ 주문 생성 → (트랜잭션 밖) 결제 API 호출 → 주문 확정(재고 처리)으로 단계 분리해 DB 커넥션 점유 최소화
+ 5xx / 네트워크 오류는 Spring Retry로 최대 3회 시도, 4xx는 재시도 없이 즉시 실패 처리
+ 재시도 실패 시 보상 트랜잭션으로 결제 취소 + 주문 취소
+ 보상까지 실패하면 Redis Sorted Set에 적재 후 1분 주기 스케줄러가 지수 백오프로 재시도(최대 5회, 이후 수동 처리)
+ 결과 : DB 커넥션 점유 시간 감소, 일시적 네트워크 오류 및 보상 실패까지 단계적 대응

---

### 4. AOP + 비동기 활동 로그

**배경**
- 위스키 조회/검색 등 사용자 활동 이력 수집 필요
- 로깅 로직이 비즈니스 코드에 섞이는 문제와, 동기 저장으로 인한 응답 지연 우려

**최종 결정**
+ `@ActivityLog` 커스텀 애너테이션 + AOP(`@Around`)로 로깅을 비즈니스 코드와 분리
+ `@TargetId` 파라미터로 대상 ID 추출, SecurityContext에서 회원 ID 추출
+ `@Async`(전용 스레드풀) + `REQUIRES_NEW` 트랜잭션으로 비동기 저장 → 본 요청 지연 없음
+ 로그 저장 실패를 예외 격리하여 본 기능에 영향 없음

---

## 📊 성능 테스트

**목표 TPS 산정 (ADR 기반)**

| 항목 | 수치 |
|------|------|
| DAU | 1만명 |
| 1인당 평균 요청 | 10회 |
| 피크 집중률 | 20% (1시간 집중) |
| 기준 TPS | 5.6 TPS |
| 목표 TPS (3배 여유) | **17 TPS** |

**인프라 스펙**

| 서비스 | 스펙 |
|--------|------|
| EC2 | t3.micro |
| RDS | db.t4g.micro (HikariCP 10, 이론상 최대 300 TPS) |
| ElastiCache | Redis (JWT 블랙리스트, 주문 만료, 결제 재시도, 평점 집계) |

+ **성능 테스트 도구 : K6**
    - 초기에는 Artillery를 검토했으나, 단계적 부하(ramp-up)·조건 분기 등 시나리오를 코드로 표현하기에 K6가 더 적합하다고 판단해 전환
    - JavaScript 기반 시나리오로 복잡한 사용자 흐름 작성 및 임계값(threshold) 검증 용이
+ K6 부하 테스트 시나리오 작성 진행 중 (`feature/k6-load-test`)

---

## 📄 ADR (Architecture Decision Records)

+ [성능 테스트 도구 선택](docs/adr/performance-test/001-performance-test-tool.md)
+ [모니터링 전략](docs/adr/performance-test/002-monitoring-strategy.md)
+ [목표 TPS 산정](docs/adr/performance-test/003-tps-target-definition.md)
