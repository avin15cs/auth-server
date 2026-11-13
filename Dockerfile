FROM node:22.22.2-alpine3.23 AS admin-ui-build
WORKDIR /workspace/admin-ui
COPY admin-ui/package.json admin-ui/package-lock.json ./
RUN npm ci
COPY admin-ui/ ./
RUN npm run build

FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
COPY --from=admin-ui-build /workspace/admin-ui/dist ./src/main/resources/static
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21.0.13_9-jre-alpine-3.24
RUN addgroup -S auth && adduser -S -G auth auth
WORKDIR /app
COPY --from=build /workspace/target/auth-server-*.jar app.jar
USER auth:auth
EXPOSE 9000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
