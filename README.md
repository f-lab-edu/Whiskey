
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
| Framework | Spring Boot 3.5.0, Spring Security, Spring Batch |
| ORM | Spring Data JPA, QueryDSL |
| DB | MySQL 8.0, Flyway |
| Cache | Redis 7 |
| Auth | JWT (JJWT 0.11.5) |
| Payment | Toss Payments API |
| Infra | Docker, AWS EC2, AWS RDS, AWS ElastiCache |
| CI/CD | GitHub Actions, GHCR |
| Test | JUnit 5, Mockito, H2 |
| Docs | Springdoc OpenAPI (Swagger) |

---

## 📐 아키텍처 구조

```
whiskey-service/
├── module-api      (Controller, Security, Batch, Spring Boot Entry Point)
├── module-domain   (Entity, Service, Repository)
├── module-payment  (Toss Payments API Client)
└── module-common   (API Response, Error Code, Shared Utilities)
```

**모듈 의존 관계**
```
module-api → module-domain, module-common, module-payment
module-domain → module-common, module-payment
module-payment → module-common
module-common → (standalone)
```

---

## 📌 주요 기능 & API

### 위스키
| Method | URI | 설명 |
|--------|-----|------|
| `GET` | `/api/whiskey` | 목록 조회 (커서 기반 페이지네이션, 필터링) |
| `GET` | `/api/whiskey/{id}` | 단건 조회 |
| `POST` | `/api/whiskey` | 등록 (ADMIN) |
| `PUT` | `/api/whiskey/{id}` | 수정 (ADMIN) |
| `DELETE` | `/api/whiskey/{id}` | 삭제 (ADMIN) |

### 회원 & 인증
| Method | URI | 설명 |
|--------|-----|------|
| `POST` | `/api/members` | 회원가입 |
| `POST` | `/api/auth/login` | 로그인 (Access + Refresh Token 발급) |
| `POST` | `/api/auth/token/refresh` | Access Token 재발급 |

### 리뷰
| Method | URI | 설명 |
|--------|-----|------|
| `POST` | `/api/reviews` | 리뷰 작성 |
| `PUT` | `/api/reviews/{id}` | 리뷰 수정 |
| `DELETE` | `/api/reviews/{id}` | 리뷰 삭제 |
| `GET` | `/api/whiskey/{id}/reviews` | 위스키별 리뷰 조회 |

### 주문 & 결제
| Method | URI | 설명 |
|--------|-----|------|
| `POST` | `/api/order` | 주문 생성 (재고 예약) |
| `PATCH` | `/api/order/{orderId}/cancel` | 주문 취소 |
| `POST` | `/api/payments/prepare` | 결제 준비 (orderId 발급) |
| `POST` | `/api/payments/confirm` | 결제 확정 |

---

## 🎯 프로젝트 목표

+ 객체지향 설계원칙을 적용하여 유지보수에 용이한 코드 구현
+ 서버 구축과 CI/CD 자동화를 통한 인프라와 배포 전반에 대한 이해
+ 트레이드오프를 고려한 기술 선택

---

## 🔧 기술적 구현

### 1. Redis Sorted Set 기반 예약 만료 처리

**배경**
- 결제 예약 후 10분 이내 미결제 시 자동 취소 필요
- 미결제 예약이 재고를 점유하는 문제 방지

**방안 비교**

| | DB 스케줄러 | Quartz | Redis Sorted Set ✅ |
|---|---|---|---|
| 정확도 | 낮음 (최대 N초 지연) | 높음 | 높음 |
| DB 부하 | 있음 (폴링) | 있음 | 없음 |
| 복잡도 | 낮음 | 높음 | 중간 |
| 장애 내성 | 높음 | 높음 | DB 백업으로 보완 |

**최종 결정**
+ 만료 시간을 score로 Redis Sorted Set에 저장, 1분마다 현재 시간 이하 score 일괄 처리
+ Redis 장애 대비 DB에도 만료시간 저장 → DB 폴링으로 복구 가능
+ 결과 : DB 부하 없이 안정적인 만료 처리, 평균 지연 < 1분

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
+ 리뷰 저장 트랜잭션 커밋 후 `AFTER_COMMIT` 이벤트 발행
+ Redis 업데이트 실패 시 다음 조회 시 DB에서 재계산하여 캐시 갱신
+ 결과 : 리뷰 작성 실패율 0% (Redis 장애와 무관)

---

### 3. 결제 API 트랜잭션 분리 + 재시도

**배경**
- Toss Payments API 호출이 네트워크 상태에 따라 지연됨
- 주문 트랜잭션 내부에서 외부 API 호출 → DB 커넥션 점유 → 커넥션 풀 고갈 위험

**최종 결정**
+ 주문 생성 → 결제 API 호출 → 주문 확정(재고 처리) 3단계로 트랜잭션 분리
+ Spring Retry로 결제 API 최대 3회 재시도 (타임아웃 시)
+ 재시도 전부 실패 시 보상 트랜잭션으로 주문 취소 처리
+ 결과 : DB 커넥션 점유 시간 감소, 일시적 네트워크 오류 대응 가능

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

**성능 테스트 도구 : Artillery** (nGrinder 대비 설정 비용 낮음, YAML 기반 시나리오)

---

## 📄 ADR (Architecture Decision Records)

+ [성능 테스트 도구 선택](docs/adr/performance-test/001-performance-test-tool.md)
+ [모니터링 전략](docs/adr/performance-test/002-monitoring-strategy.md)
+ [목표 TPS 산정](docs/adr/performance-test/003-tps-target-definition.md)
