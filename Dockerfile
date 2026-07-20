# 1단계: 빌드 스테이지 (이미 GitHub Actions에서 빌드하므로 생략 가능)
FROM eclipse-temurin:21-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사
COPY module-api/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
# 프로필은 컨테이너 부팅 시 SPRING_PROFILES_ACTIVE env 로 주입 (CD 파이프라인 --env-file 채널로 통일)
# env 미주입 시 application.yml 의 spring.profiles.active: local 로 폴백
ENTRYPOINT ["java", "-jar", "app.jar"]