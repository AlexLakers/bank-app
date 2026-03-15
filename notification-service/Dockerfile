FROM amazoncorretto:21-alpine AS base

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

COPY dto-exceptions-common dto-exceptions-common
COPY notification-service notification-service

RUN chmod +x gradlew

RUN ./gradlew :notification-service:build --no-daemon -x test -x contractTest

FROM amazoncorretto:21-alpine AS runtime

WORKDIR /app

COPY --from=base /app/notification-service/build/libs/notification-service-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]