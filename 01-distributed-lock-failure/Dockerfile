
# One Dockerfile for every app in the build; compose picks the app with MODULE.
ARG MODULE

FROM maven:3.9.16-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build

COPY pom.xml ./
COPY payout-platform/pom.xml payout-platform/
COPY payout-worker/pom.xml payout-worker/
COPY fake-bank/pom.xml fake-bank/
RUN mvn -B -ntp -pl ${MODULE} -am dependency:go-offline

COPY ${MODULE}/src/ ${MODULE}/src/
RUN mvn -B -ntp -pl ${MODULE} -am -DskipTests package

RUN java -Djarmode=tools -jar ${MODULE}/target/*.jar \
        extract --layers --launcher --destination extracted


FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Unprivileged user: nothing here needs root.
RUN addgroup -S spring && adduser -S -G spring spring
USER spring:spring

COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
