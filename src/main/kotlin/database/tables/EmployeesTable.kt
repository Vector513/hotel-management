package database.tables

import org.jetbrains.exposed.sql.Table

object EmployeesTable : Table("employees") {
    val employeeId = integer("employee_id").autoIncrement()
    val fullName = varchar("full_name", 100)
    val floor = integer("floor")

    override val primaryKey = PrimaryKey(employeeId)
}
