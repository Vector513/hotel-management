package com.example

import com.example.database.tables.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    // Получаем параметры подключения из переменных окружения или используем значения по умолчанию
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/hotel_db"
    val dbUser = System.getenv("DB_USER") ?: "hotel_admin"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "orf.12014319N"
    val dbDriver = System.getenv("DB_DRIVER") ?: "org.postgresql.Driver"
    
    try {
        val database = Database.connect(
            url = dbUrl,
            user = dbUser,
            driver = dbDriver,
            password = dbPassword,
        )
        log.info("Successfully connected to database: $dbUrl")
        
        // Создаем таблицы, если их еще нет
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                RoomsTable,
                UsersTable,
                EmployeesTable,
                ClientsTable,
                InvoicesTable,
                CleaningScheduleTable
            )
        }
        log.info("Database schema initialized")
    } catch (e: Exception) {
        log.error("Failed to connect to database: ${e.message}", e)
        throw e
    }
}
