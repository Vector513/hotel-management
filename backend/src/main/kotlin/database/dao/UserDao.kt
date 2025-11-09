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
    fun create(
        username: String,
        password: String,
        role: UserRole,
        clientId: Int? = null,
        employeeId: Int? = null
    ): User? = transaction {
        val hash = hashPassword(password)
        val insertedId = UsersTable.insert {
            it[UsersTable.username] = username
            it[UsersTable.passwordHash] = hash
            it[UsersTable.role] = role.name
            it[UsersTable.clientId] = clientId
            it[UsersTable.employeeId] = employeeId
        } get UsersTable.id

        findById(insertedId)
    }

    // Создать пользователя для клиента
    fun createForClient(clientId: Int, username: String, password: String): User? {
        return create(username, password, UserRole.CLIENT, clientId = clientId)
    }

    // Создать пользователя для сотрудника
    fun createForEmployee(employeeId: Int, username: String, password: String): User? {
        return create(username, password, UserRole.WORKER, employeeId = employeeId)
    }

    // Обновить существующего пользователя
    fun update(id: Int, updated: User): Boolean = transaction {
        UsersTable.update({ UsersTable.id eq id }) {
            it[username] = updated.username
            it[passwordHash] = updated.passwordHash
            it[role] = updated.role.name
            it[clientId] = updated.clientId
            it[employeeId] = updated.employeeId
        } > 0
    }

    // Удалить пользователя
    fun delete(id: Int): Boolean = transaction {
        UsersTable.deleteWhere { UsersTable.id eq id } > 0
    }

    // Удалить пользователя по clientId
    fun deleteByClientId(clientId: Int) = transaction {
        UsersTable.deleteWhere { UsersTable.clientId eq clientId }
    }

    // Удалить пользователя по employeeId
    fun deleteByEmployeeId(employeeId: Int) = transaction {
        UsersTable.deleteWhere { UsersTable.employeeId eq employeeId }
    }

    // Найти пользователя по clientId
    fun findByClientId(clientId: Int): User? = transaction {
        UsersTable.selectAll()
            .map { it.toUser() }
            .singleOrNull { it.clientId == clientId }
    }

    // Найти пользователя по employeeId
    fun findByEmployeeId(employeeId: Int): User? = transaction {
        UsersTable.selectAll()
            .map { it.toUser() }
            .singleOrNull { it.employeeId == employeeId }
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
        role = UserRole.valueOf(this[UsersTable.role]),
        clientId = this[UsersTable.clientId],
        employeeId = this[UsersTable.employeeId]
    )

    // Хешируем пароль через BCrypt
    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
}
