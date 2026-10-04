
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src/ src/
RUN mvn -B -ntp -DskipTests package

RUN java -Djarmode=tools -jar target/*.jar \
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

EXPOSE 8101
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
