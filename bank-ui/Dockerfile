FROM amazoncorretto:21-alpine AS base

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

COPY dto-exceptions-common dto-exceptions-common
COPY bank-ui bank-ui

RUN chmod +x gradlew

RUN ./gradlew :bank-ui:build --no-daemon -x test

FROM amazoncorretto:21-alpine AS runtime

WORKDIR /app

COPY --from=base /app/bank-ui/build/libs/bank-ui-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]