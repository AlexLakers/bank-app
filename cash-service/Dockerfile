FROM amazoncorretto:21-alpine AS base

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

COPY dto-exceptions-common dto-exceptions-common
COPY cash-service cash-service

RUN chmod +x gradlew

RUN ./gradlew :cash-service:build --no-daemon -x test

FROM amazoncorretto:21-alpine AS runtime

WORKDIR /app

COPY --from=base /app/cash-service/build/libs/cash-service-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]