package com.example.database.dao

import com.example.database.tables.EmployeesTable
import com.example.models.Employee
import com.example.models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class EmployeeDao {

    fun getAll(): List<Employee> = transaction {
        EmployeesTable.selectAll().map { it.toEmployee() }
    }

    fun findById(id: Int): Employee? = transaction {
        EmployeesTable
            .selectAll()
            .where { EmployeesTable.employeeId eq id }
            .map { it.toEmployee() }
            .singleOrNull()
    }

    fun findByUser(user: User): Employee? {
        val employeeId = user.employeeId ?: return null
        return findById(employeeId)
    }

    fun findByFloor(floor: Int): List<Employee> = transaction {
        EmployeesTable
            .selectAll()
            .where { EmployeesTable.floor eq floor }
            .map { it.toEmployee() }
    }

    fun add(employee: Employee): Int = transaction {
        EmployeesTable.insert {
            it[fullName] = employee.fullName
            it[floor] = employee.floor
        } get EmployeesTable.employeeId
    }

    fun update(id: Int, updated: Employee): Boolean = transaction {
        EmployeesTable.update({ EmployeesTable.employeeId eq id }) {
            it[fullName] = updated.fullName
            it[floor] = updated.floor
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        EmployeesTable.deleteWhere { EmployeesTable.employeeId eq id } > 0
    }

    fun fireEmployee(id: Int): Boolean = delete(id)

    private fun ResultRow.toEmployee() = Employee(
        employeeId = this[EmployeesTable.employeeId],
        fullName = this[EmployeesTable.fullName],
        floor = this[EmployeesTable.floor]
    )
}
