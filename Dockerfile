# 1단계: 빌드 스테이지
FROM gradle:8.13-jdk21 AS builder

WORKDIR /app

# Gradle 캐싱 최적화를 위해 의존성 파일만 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
COPY module-api/build.gradle ./module-api/
COPY module-domain/build.gradle ./module-domain/
COPY module-common/build.gradle ./module-common/

# 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY module-api ./module-api
COPY module-domain ./module-domain
COPY module-common ./module-common

# 빌드 실행 (테스트 제외)
RUN gradle :module-api:build -x test --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 빌드 스테이지에서 jar 파일만 복사
COPY --from=builder /app/module-api/build/libs/*-SNAPSHOT.jar app.jar

# 포트 노출
EXPOSE 8080

# 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]