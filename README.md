# Hotel Management System

Система управления отелем с веб-интерфейсом для администраторов, клиентов и работников.

## Технологии

### Backend
- **Kotlin** + **Ktor** - веб-фреймворк
- **PostgreSQL** - база данных
- **Exposed** - ORM
- **JWT** - аутентификация
- **BCrypt** - хеширование паролей

### Frontend
- **React 18** + **TypeScript**
- **Material-UI (MUI)** - UI компоненты
- **React Router** - маршрутизация
- **Axios** - HTTP клиент
- **Vite** - сборщик

## Быстрый старт

### С Docker (рекомендуется)

1. Клонируйте репозиторий
2. Создайте `.env` файл из `env.example`
3. Запустите:
   ```bash
   docker-compose up -d
   ```
4. Откройте http://localhost:3000

Подробнее в [DEPLOYMENT.md](./DEPLOYMENT.md)

### Без Docker

#### Backend
```bash
cd backend
./gradlew run
```

#### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Структура проекта

```
hotel-management/
├── backend/              # Kotlin/Ktor backend
│   ├── src/main/kotlin/
│   │   ├── routes/      # API маршруты
│   │   ├── models/      # Модели данных
│   │   ├── database/    # DAO и таблицы
│   │   └── auth/        # JWT аутентификация
│   └── Dockerfile
├── frontend/            # React frontend
│   ├── src/
│   │   ├── components/  # React компоненты
│   │   ├── services/    # API клиент
│   │   └── types/       # TypeScript типы
│   └── Dockerfile
├── docker-compose.yml   # Docker Compose конфигурация
└── DEPLOYMENT.md        # Документация по развертыванию
```

## Функциональность

### Администратор
- Управление клиентами, номерами, сотрудниками
- Создание и управление счетами
- Управление расписанием уборки
- Управление пользователями
- Просмотр квартальных отчетов

### Клиент
- Просмотр своих счетов
- Запрос нового счета
- Узнать уборщика своего номера

### Работник
- Просмотр своего расписания уборки
- Узнать уборщика номера клиента

## Учетные данные по умолчанию

После заполнения базы данных тестовыми данными:

- **Администратор**: `admin` / `admin123`
- **Клиенты**: `client1`, `client2`, ... / `password123`
- **Работники**: `worker1`, `worker2`, ... / `password123`

## Заполнение базы данных

```bash
# С Docker
docker-compose exec backend ./gradlew run --args="com.example.scripts.SeedDatabaseKt"

# Без Docker
cd backend
./gradlew run --args="com.example.scripts.SeedDatabaseKt"
```

## Разработка

### Backend
```bash
cd backend
./gradlew build
./gradlew run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Лицензия

MIT
