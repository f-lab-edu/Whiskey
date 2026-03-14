## 모니터링 전략 선택

+ 배경 : 성능 테스트 중 서버 상태 모니터링 필요
+ 고려한 옵션
  - Grafana + Prometheus
  - Spring Actuator + AWS 콘솔
+ 결정 : Spring Actuator + AWS 콘솔 + MySQL log
+ 이유
  - 프리티어 메모리 제약으로 별도 모니터링 서버 운영 부담
  - 일회성 테스트 목적에 오버스펙