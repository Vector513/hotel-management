package database.dao

import database.tables.UsersTable
import models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest

class UserDao {

    fun getAll(): List<User> = transaction {
        UsersTable.selectAll().map { it.toUser() }
    }

    fun findById(id: Int): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun findByUsername(username: String): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.username eq username }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun create(username: String, password: String, role: String): Int = transaction {
        UsersTable.insert {
            it[UsersTable.username] = username
            it[UsersTable.passwordHash] = hashPassword(password)
            it[UsersTable.role] = role
        } get UsersTable.id
    }

    fun update(id: Int, updated: User): Boolean = transaction {
        UsersTable.update({ UsersTable.id eq id }) {
            it[username] = updated.username
            it[passwordHash] = updated.passwordHash
            it[role] = updated.role
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        UsersTable.deleteWhere { UsersTable.id eq id } > 0
    }

    fun verifyPassword(user: User, password: String): Boolean {
        return BCrypt.checkpw(password, user.passwordHash)
    }

    // --- приватный mapper ---
    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        username = this[UsersTable.username],
        passwordHash = this[UsersTable.passwordHash],
        role = this[UsersTable.role]
    )

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
