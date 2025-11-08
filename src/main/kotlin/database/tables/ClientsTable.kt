package database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object ClientsTable : Table("clients") {
    val clientId = integer("client_id").autoIncrement()
    val passportNumber = varchar("passport_number", 20).uniqueIndex()
    val fullName = varchar("full_name", 100)
    val city = varchar("city", 50)
    val checkInDate = date("check_in_date")
    val daysReserved = integer("days_reserved")
    val roomId = integer("room_id").references(RoomsTable.roomId)

    override val primaryKey = PrimaryKey(clientId)
}
