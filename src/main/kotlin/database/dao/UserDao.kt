package com.example.database.dao

import com.example.database.tables.UsersTable
import com.example.models.User
import com.example.models.UserRole
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.mindrot.jbcrypt.BCrypt

class UserDao {

    // Получить всех пользователей
    fun getAll(): List<User> = transaction {
        UsersTable.selectAll().map { it.toUser() }
    }

    // Найти пользователя по ID
    fun findById(id: Int): User? = transaction {
        UsersTable.selectAll()
            .map { it.toUser() }
            .singleOrNull { it.id == id }
    }

    // Найти пользователя по username
    fun findByUsername(username: String): User? = transaction {
        UsersTable.selectAll()
            .map { it.toUser() }
            .singleOrNull { it.username == username }
    }

    // Создать нового пользователя
    fun create(username: String, password: String, role: UserRole): User? = transaction {
        val hash = hashPassword(password)
        val insertedId = UsersTable.insert {
            it[UsersTable.username] = username
            it[UsersTable.passwordHash] = hash
            it[UsersTable.role] = role.name
        } get UsersTable.id

        findById(insertedId)
    }

    // Обновить существующего пользователя
    fun update(id: Int, updated: User): Boolean = transaction {
        UsersTable.update({ UsersTable.id eq id }) {
            it[username] = updated.username
            it[passwordHash] = updated.passwordHash
            it[role] = updated.role.name
        } > 0
    }

    // Удалить пользователя
    fun delete(id: Int): Boolean = transaction {
        UsersTable.deleteWhere { UsersTable.id eq id } > 0
    }

    // Проверка пароля через BCrypt
    fun verifyPassword(user: User, password: String): Boolean {
        return BCrypt.checkpw(password, user.passwordHash)
    }

    // --- приватный mapper ---
    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        username = this[UsersTable.username],
        passwordHash = this[UsersTable.passwordHash],
        role = UserRole.valueOf(this[UsersTable.role])
    )

    // Хешируем пароль через BCrypt
    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
}
