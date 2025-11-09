# Как заполнить базу данных тестовыми данными

## Простая инструкция

### Шаг 1: Убедитесь, что контейнеры запущены

Откройте PowerShell в папке проекта и выполните:

```powershell
docker-compose ps
```

Все контейнеры должны быть в статусе `Up`. Если нет, запустите:

```powershell
docker-compose up -d
```

### Шаг 2: Пересоберите backend (если нужно)

Если вы только что изменили код скрипта, пересоберите backend:

```powershell
docker-compose up -d --build backend
```

### Шаг 3: Заполните базу данных

**Способ 1 (рекомендуется):** Войдите в контейнер и выполните команды:

```powershell
# Войти в контейнер backend
docker-compose exec backend sh

# Внутри контейнера выполните эти команды (по одной):
export DB_URL=jdbc:postgresql://postgres:5432/hotel_db
export DB_USER=hotel_admin
export DB_PASSWORD=orf.12014319N
export DB_DRIVER=org.postgresql.Driver

# Проверьте, что переменные установлены:
echo "DB_URL=$DB_URL"

# Запустите скрипт (теперь значение по умолчанию уже правильное):
java -cp app.jar com.example.scripts.SeedDatabaseKt

# После выполнения выйдите из контейнера:
exit
```

**Важно:** Убедитесь, что вы выполнили все команды `export` перед запуском `java`. Переменные должны быть установлены в той же сессии shell.

**Способ 2:** Выполните одной командой (может не работать в PowerShell):

```powershell
docker-compose exec backend sh -c "export DB_URL=jdbc:postgresql://postgres:5432/hotel_db && export DB_USER=hotel_admin && export DB_PASSWORD=orf.12014319N && export DB_DRIVER=org.postgresql.Driver && java -cp app.jar com.example.scripts.SeedDatabaseKt"
```

### Что произойдет

Скрипт создаст:
- ✅ 1 администратора (логин: `admin`, пароль: `admin123`)
- ✅ 30 номеров отеля (на 5 этажах)
- ✅ 10 работников (логины: `worker1`, `worker2`, ... пароль: `password123`)
- ✅ Расписание уборки для работников
- ✅ 50 клиентов (логины: `client1`, `client2`, ... пароль: `password123`)
- ✅ Счета для некоторых клиентов

### После заполнения

Вы увидите сообщение:
```
==================================================
ГОТОВО!
==================================================
```

### Данные для входа

После заполнения БД вы можете войти в систему:

- **Администратор**: 
  - Логин: `admin`
  - Пароль: `admin123`

- **Клиенты**: 
  - Логины: `client1`, `client2`, `client3`, ... (до `client50`)
  - Пароль: `password123`

- **Работники**: 
  - Логины: `worker1`, `worker2`, ... (до `worker10`)
  - Пароль: `password123`

## Если возникли ошибки

### Ошибка: "Connection refused" или "database does not exist"

1. Проверьте, что PostgreSQL запущен:
   ```powershell
   docker-compose ps postgres
   ```

2. Проверьте логи PostgreSQL:
   ```powershell
   docker-compose logs postgres
   ```

3. Перезапустите контейнеры:
   ```powershell
   docker-compose restart
   ```

### Ошибка: "Unable to access jarfile app.jar"

1. Проверьте, что backend контейнер запущен:
   ```powershell
   docker-compose ps backend
   ```

2. Пересоберите контейнеры:
   ```powershell
   docker-compose down
   docker-compose up -d --build
   ```

## Альтернативный способ (если первый не работает)

Если команда выше не работает, попробуйте войти в контейнер и запустить скрипт оттуда:

```powershell
# Войти в контейнер backend
docker-compose exec backend sh

# Внутри контейнера выполнить:
export DB_URL=jdbc:postgresql://postgres:5432/hotel_db
export DB_USER=hotel_admin
export DB_PASSWORD=orf.12014319N
export DB_DRIVER=org.postgresql.Driver
java -cp app.jar com.example.scripts.SeedDatabaseKt

# Выйти из контейнера
exit
```

