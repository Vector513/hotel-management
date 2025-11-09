package com.example.database.dao

import com.example.database.tables.ClientsTable
import com.example.models.Client
import com.example.models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

class ClientDao {

    fun getAll(): List<Client> = transaction {
        ClientsTable.selectAll().map { it.toClient() }
    }

    fun findById(id: Int): Client? = transaction {
        ClientsTable
            .selectAll()
            .where { ClientsTable.clientId eq id }
            .map { it.toClient() }
            .singleOrNull()
    }

    fun findByUser(user: User): Client? {
        val clientId = user.clientId ?: return null
        return findById(clientId)
    }

    fun findByPassport(passport: String): Client? = transaction {
        ClientsTable
            .selectAll()
            .where { ClientsTable.passportNumber eq passport }
            .map { it.toClient() }
            .singleOrNull()
    }

    fun findByRoom(roomId: Int): List<Client> = transaction {
        ClientsTable
            .selectAll()
            .where { ClientsTable.roomId eq roomId }
            .map { it.toClient() }
    }

    fun add(client: Client): Int = transaction {
        ClientsTable.insert {
            it[passportNumber] = client.passportNumber
            it[fullName] = client.fullName
            it[city] = client.city
            it[checkInDate] = client.checkInDate
            it[daysReserved] = client.daysReserved
            it[roomId] = client.roomId
        } get ClientsTable.clientId
    }

    fun update(id: Int, updated: Client): Boolean = transaction {
        ClientsTable.update({ ClientsTable.clientId eq id }) {
            it[passportNumber] = updated.passportNumber
            it[fullName] = updated.fullName
            it[city] = updated.city
            it[checkInDate] = updated.checkInDate
            it[daysReserved] = updated.daysReserved
            it[roomId] = updated.roomId
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        ClientsTable.deleteWhere { ClientsTable.clientId eq id } > 0
    }

    fun clearRoom(roomId: Int): Boolean = transaction {
        ClientsTable.deleteWhere { ClientsTable.roomId eq roomId } > 0
    }

    fun findByCheckoutDate(date: LocalDate): List<Client> = transaction {
        ClientsTable
            .selectAll()
            .map { it.toClient() }
            .filter { it.checkInDate.plusDays(it.daysReserved.toLong()) == date }
    }

    fun setResidentsByRoom(roomId: Int, isResident: Boolean) = transaction {
        ClientsTable.update({ ClientsTable.roomId eq roomId }) {
            it[ClientsTable.isResident] = isResident
        }
    }


    private fun ResultRow.toClient() = Client(
        clientId = this[ClientsTable.clientId],
        passportNumber = this[ClientsTable.passportNumber],
        fullName = this[ClientsTable.fullName],
        city = this[ClientsTable.city],
        checkInDate = this[ClientsTable.checkInDate],
        daysReserved = this[ClientsTable.daysReserved],
        roomId = this[ClientsTable.roomId],
        isResident = this[ClientsTable.isResident]
    )

}
