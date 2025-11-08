package database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date


object InvoicesTable : Table("invoices") {
    val invoiceId = integer("invoice_id").autoIncrement()
    val clientId = integer("client_id").references(ClientsTable.clientId)
    val totalAmount = decimal("total_amount", 10, 2)
    val issueDate = date("issue_date")

    override val primaryKey = PrimaryKey(invoiceId)
}
