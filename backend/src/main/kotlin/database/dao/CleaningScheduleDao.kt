package com.example.database.dao

import com.example.database.tables.CleaningScheduleTable
import com.example.models.CleaningSchedule
import com.example.models.DayOfWeek
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class CleaningScheduleDao {

    fun getAll(): List<CleaningSchedule> = transaction {
        CleaningScheduleTable.selectAll().map { it.toSchedule() }
    }

    fun findById(id: Int): CleaningSchedule? = transaction {
        CleaningScheduleTable
            .selectAll()
            .where { CleaningScheduleTable.scheduleId eq id }
            .map { it.toSchedule() }
            .singleOrNull()
    }

    fun findByEmployee(employeeId: Int): List<CleaningSchedule> = transaction {
        CleaningScheduleTable
            .selectAll()
            .where { CleaningScheduleTable.employeeId eq employeeId }
            .map { it.toSchedule() }
    }

    fun findByDay(day: DayOfWeek): List<CleaningSchedule> = transaction {
        CleaningScheduleTable
            .selectAll()
            .where { CleaningScheduleTable.dayOfWeek eq day.name }
            .map { it.toSchedule() }
    }

    fun add(schedule: CleaningSchedule): Int = transaction {
        CleaningScheduleTable.insert {
            it[employeeId] = schedule.employeeId
            it[floor] = schedule.floor
            it[dayOfWeek] = schedule.dayOfWeek.name
        } get CleaningScheduleTable.scheduleId
    } ?: -1

    fun update(id: Int, updated: CleaningSchedule): Boolean = transaction {
        CleaningScheduleTable.update({ CleaningScheduleTable.scheduleId eq id }) {
            it[employeeId] = updated.employeeId
            it[floor] = updated.floor
            it[dayOfWeek] = updated.dayOfWeek.name
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        CleaningScheduleTable.deleteWhere { CleaningScheduleTable.scheduleId eq id } > 0
    }

    private fun ResultRow.toSchedule() = CleaningSchedule(
        scheduleId = this[CleaningScheduleTable.scheduleId],
        employeeId = this[CleaningScheduleTable.employeeId],
        floor = this[CleaningScheduleTable.floor],
        dayOfWeek = DayOfWeek.valueOf(this[CleaningScheduleTable.dayOfWeek])
    )
}
