package com.example.database.tables

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20) // ADMIN / CLIENT / WORKER
    val clientId = integer("client_id").nullable() // новая колонка для связи с клиентом

    override val primaryKey = PrimaryKey(id)
}
