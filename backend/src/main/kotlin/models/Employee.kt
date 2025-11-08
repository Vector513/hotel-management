package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Employee(
    val employeeId: Int,
    val fullName: String,
    val floor: Int
)

@Serializable
data class CreateEmployeeRequest(
    val fullName: String,
    val floor: Int
)

@Serializable
data class UpdateEmployeeRequest(
    val fullName: String,
    val floor: Int
) {
    fun toEmployee(employeeId: Int): Employee = Employee(
        employeeId = employeeId,
        fullName = fullName,
        floor = floor
    )
}