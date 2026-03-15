FROM amazoncorretto:21-alpine AS base

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

COPY dto-exceptions-common dto-exceptions-common
COPY transfer-service transfer-service

RUN chmod +x gradlew

RUN ./gradlew :transfer-service:build --no-daemon -x test

FROM amazoncorretto:21-alpine AS runtime

WORKDIR /app

COPY --from=base /app/transfer-service/build/libs/transfer-service-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]