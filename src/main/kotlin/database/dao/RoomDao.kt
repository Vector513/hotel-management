package database.dao

import database.tables.RoomsTable
import models.Room
import models.RoomType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

class RoomDao {

    fun getAll(): List<Room> = transaction {
        RoomsTable.selectAll().map { it.toRoom() }
    }

    fun findById(id: Int): Room? = transaction {
        RoomsTable
            .selectAll()
            .where { RoomsTable.roomId eq id }
            .map { it.toRoom() }
            .singleOrNull()
    }

    fun add(room: Room): Int = transaction {
        RoomsTable.insert {
            it[roomNumber] = room.roomNumber
            it[floor] = room.floor
            it[type] = room.type.name
            it[pricePerDay] = room.pricePerDay
            it[phoneNumber] = room.phoneNumber
        } get RoomsTable.roomId
    } ?: -1

    fun update(id: Int, updated: Room): Boolean = transaction {
        RoomsTable.update({ RoomsTable.roomId eq id }) {
            it[roomNumber] = updated.roomNumber
            it[floor] = updated.floor
            it[type] = updated.type.name
            it[pricePerDay] = updated.pricePerDay
            it[phoneNumber] = updated.phoneNumber
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        RoomsTable.deleteWhere { RoomsTable.roomId eq id } > 0
    }

    private fun ResultRow.toRoom() = Room(
        roomId = this[RoomsTable.roomId],
        roomNumber = this[RoomsTable.roomNumber],
        floor = this[RoomsTable.floor],
        type = RoomType.valueOf(this[RoomsTable.type]),
        pricePerDay = this[RoomsTable.pricePerDay],
        phoneNumber = this[RoomsTable.phoneNumber]
    )
}
