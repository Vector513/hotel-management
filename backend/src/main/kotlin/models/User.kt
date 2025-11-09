package com.example.models

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole { ADMIN, WORKER, CLIENT }

@Serializable
data class User(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val role: UserRole,
    val clientId: Int? = null,
    val employeeId: Int? = null // новая колонка для связи с сотрудником
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val password: String,
    val role: UserRole
)

@Serializable
data class UpdateUserRequest(
    val username: String,
    val password: String,
    val role: UserRole
) {
    fun toUser(id: Int, clientId: Int? = null, employeeId: Int? = null): User = User(
        id = id,
        username = username,
        passwordHash = password,
        role = role,
        clientId = clientId,
        employeeId = employeeId
    )
}
