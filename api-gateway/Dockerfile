FROM amazoncorretto:21-alpine AS base

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

#COPY dto-exceptions-common dto-exceptions-common
COPY api-gateway api-gateway

RUN chmod +x gradlew

RUN ./gradlew :api-gateway:build --no-daemon -x test

FROM amazoncorretto:21-alpine AS runtime

WORKDIR /app

COPY --from=base /app/api-gateway/build/libs/api-gateway-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]