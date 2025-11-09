# Stage 1: Build
FROM gradle:8.11-jdk17 AS builder

WORKDIR /app

# Копируем файлы сборки
COPY gradle/ ./gradle/
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY gradle.properties ./

# Копируем исходный код
COPY src/ ./src/

# Собираем приложение используя gradle из образа (Ktor plugin создает fat JAR с манифестом)
RUN gradle build --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Копируем все JAR файлы и выбираем нужный
COPY --from=builder /app/build/libs/*.jar /tmp/
RUN cd /tmp && \
    if [ -f app.jar ]; then \
        mv app.jar /app/app.jar; \
    elif ls backend-*-all.jar 1> /dev/null 2>&1; then \
        mv backend-*-all.jar /app/app.jar; \
    elif ls *-all.jar 1> /dev/null 2>&1; then \
        mv *-all.jar /app/app.jar; \
    else \
        echo "No JAR file found!" && exit 1; \
    fi && \
    rm -rf /tmp/*.jar

# Создаем директорию для логов
RUN mkdir -p /app/logs

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]

