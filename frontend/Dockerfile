# Используем официальный образ Node.js
FROM node:18-alpine AS builder

# Аргумент для API URL
ARG VITE_API_URL=http://localhost:8080

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем package.json и package-lock.json
COPY package*.json ./

# Устанавливаем зависимости
RUN npm ci

# Копируем исходный код
COPY . .

# Устанавливаем переменную окружения для сборки
ENV VITE_API_URL=$VITE_API_URL

# Собираем приложение
RUN npm run build

# Используем nginx для статики
FROM nginx:alpine

# Копируем собранные файлы в nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# Копируем конфигурацию nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Открываем порт
EXPOSE 80

# Запускаем nginx
CMD ["nginx", "-g", "daemon off;"]

