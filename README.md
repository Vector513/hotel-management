# Hotel Management System

Полноценная система управления отелем с веб-интерфейсом для администраторов, клиентов и сотрудников.

## Технологии

### Backend

* **Kotlin** + **Ktor**
* **PostgreSQL**
* **Exposed** (ORM)
* **JWT** (аутентификация)
* **BCrypt** (хеширование)

### Frontend

* **React 18** + **TypeScript**
* **Material-UI (MUI)**
* **React Router**
* **Axios**
* **Vite**

---

## Быстрый старт

## ▶️ С Docker (рекомендуется)

Подходит одинаково для **Windows**, **Linux** и **macOS**.

1. Клонируйте репозиторий

   ```bash
   git clone https://github.com/username/hotel-management.git
   cd hotel-management
   ```

2. Создайте файл `.env` из `env.example`
   Обязательно укажите параметры для PostgreSQL и JWT.

3. Запустите проект:

   ```bash
   docker-compose up -d
   ```

4. Откройте в браузере:
   **[http://localhost:3000](http://localhost:3000)**

---

## ▶️ Без Docker

Инструкция разбита для **Windows** и **Linux**, поскольку команды отличаются.

---

# Backend (Kotlin / Ktor)

### 📌 Требования

* **JDK 17+**
* **PostgreSQL**
* **Gradle Wrapper** (уже включён в проект)

---

## Linux / macOS

```bash
cd backend
./gradlew run
```

---

## Windows (PowerShell / CMD)

```powershell
cd backend
gradlew.bat run
```

Если Gradle не запускается — убедитесь, что `.bat` файл исполняемый.

---

# Frontend (React)

### 📌 Требования

* **Node.js 18+**
* **npm** или **yarn**

---

## Linux / macOS

```bash
cd frontend
npm install
npm run dev
```

---

## Windows

Команды те же:

```powershell
cd frontend
npm install
npm run dev
```

Приложение будет доступно по адресу:
**[http://localhost:5173](http://localhost:5173)**

---

# Структура проекта

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
└── docker-compose.yml   # Docker Compose конфигурация
```

---

# Заполнение базы данных

## ▶️ Через Docker

```bash
docker-compose exec backend ./gradlew run --args="com.example.scripts.SeedDatabaseKt"
```

## ▶️ Без Docker

### Linux / macOS

```bash
cd backend
./gradlew run --args="com.example.scripts.SeedDatabaseKt"
```

### Windows

```powershell
cd backend
gradlew.bat run --args="com.example.scripts.SeedDatabaseKt"
```

---

# Функциональность

### 👑 Администратор

* Управление клиентами, номерами и сотрудниками
* Создание и управление счетами
* Управление расписанием уборки
* Просмотр отчетов
* Управление пользователями

### 👤 Клиент

* Просмотр счетов
* Запрос нового счета
* Просмотр ответственного уборщика

### 🧹 Работник

* Просмотр расписания уборки
* Информация об уборке номеров

---

# Учетные данные по умолчанию

После запуска скрипта SeedDatabase:

| Роль          | Логин    | Пароль      |
| ------------- | -------- | ----------- |
| Администратор | admin    | admin123    |
| Клиенты       | client1… | password123 |
| Работники     | worker1… | password123 |

---

# Разработка

## Backend

```bash
cd backend
./gradlew build   # Windows: gradlew.bat build
./gradlew run     # Windows: gradlew.bat run
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```

---

# Лицензия

MIT
