# Stage 1: Build file .jar bằng Maven và Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Khởi chạy ứng dụng với JRE 21 siêu nhẹ
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy file jar từ Stage 1 sang
COPY --from=build /app/target/*.jar app.jar
# Mở cổng 8081
EXPOSE 8081
# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]