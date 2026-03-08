FROM amazoncorretto:21-alpine AS base

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

COPY dto-exceptions-common dto-exceptions-common
COPY account-service account-service

RUN chmod +x gradlew

RUN ./gradlew :account-service:build --no-daemon -x test -x contractTest

FROM amazoncorretto:21-alpine AS runtime

WORKDIR /app

COPY --from=base /app/account-service/build/libs/account-service-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]