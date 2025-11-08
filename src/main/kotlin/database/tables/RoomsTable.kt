package database.tables

import org.jetbrains.exposed.sql.Table

object RoomsTable : Table("rooms") {
    val roomId = integer("room_id").autoIncrement()
    val roomNumber = integer("room_number").uniqueIndex()
    val floor = integer("floor")
    val type = varchar("type", 20) // SINGLE, DOUBLE, TRIPLE
    val pricePerDay = decimal("price_per_day", 10, 2)
    val phoneNumber = varchar("phone_number", 20)

    override val primaryKey = PrimaryKey(roomId)
}
