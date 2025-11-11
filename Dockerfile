FROM eclipse-temurin:17-jdk-jammy

# JAR 파일 복사
COPY build/libs/docker-ex-0.0.1-SNAPSHOT.jar /app/app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]