# Backend PF. Shoes — build bằng Maven, chạy bằng JRE 21.
# Render (free) cấp ~512MB RAM nên giới hạn heap theo % RAM của container.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -m app
COPY --from=build /src/target/*.jar app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8082
# Render truyền cổng qua biến PORT (application.properties: server.port=${PORT:8082})
CMD ["java", "-jar", "app.jar"]
