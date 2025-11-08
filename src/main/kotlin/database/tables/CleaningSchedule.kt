package database.tables

import org.jetbrains.exposed.sql.Table

object CleaningScheduleTable : Table("cleaningschedule") {
    val scheduleId = integer("schedule_id").autoIncrement()
    val employeeId = integer("employee_id").references(EmployeesTable.employeeId)
    val floor = integer("floor")
    val dayOfWeek = varchar("day_of_week", 15)

    override val primaryKey = PrimaryKey(scheduleId)
}
