package com.example.database.dao

import com.example.database.tables.InvoicesTable
import com.example.models.Invoice
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

class InvoiceDao {

    fun getAll(): List<Invoice> = transaction {
        InvoicesTable.selectAll().map { it.toInvoice() }
    }

    fun findById(id: Int): Invoice? = transaction {
        InvoicesTable
            .selectAll()
            .where { InvoicesTable.invoiceId eq id }
            .map { it.toInvoice() }
            .singleOrNull()
    }

    fun findByClient(clientId: Int): List<Invoice> = transaction {
        InvoicesTable
            .selectAll()
            .where { InvoicesTable.clientId eq clientId }
            .map { it.toInvoice() }
    }

    fun add(invoice: Invoice): Int = transaction {
        InvoicesTable.insert {
            it[clientId] = invoice.clientId
            it[totalAmount] = invoice.totalAmount
            it[issueDate] = invoice.issueDate
        } get InvoicesTable.invoiceId
    } ?: -1

    fun update(id: Int, updated: Invoice): Boolean = transaction {
        InvoicesTable.update({ InvoicesTable.invoiceId eq id }) {
            it[clientId] = updated.clientId
            it[totalAmount] = updated.totalAmount
            it[issueDate] = updated.issueDate
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        InvoicesTable.deleteWhere { InvoicesTable.invoiceId eq id } > 0
    }

    private fun ResultRow.toInvoice() = Invoice(
        invoiceId = this[InvoicesTable.invoiceId],
        clientId = this[InvoicesTable.clientId],
        totalAmount = this[InvoicesTable.totalAmount],
        issueDate = this[InvoicesTable.issueDate]
    )
}
