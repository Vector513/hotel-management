package com.example.models

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class Invoice(
    val invoiceId: Int,
    val clientId: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal,
    @Serializable(with = LocalDateSerializer::class)
    val issueDate: LocalDate
)

@Serializable
data class CreateInvoiceRequest(
    val clientId: Int,
    @Serializable(with = LocalDateSerializer::class)
    val issueDate: LocalDate? = null // Если не указана, используется сегодняшняя дата
)

@Serializable
data class InvoiceResponse(
    val invoiceId: Int,
    val amount: String
)

@Serializable
data class UpdateInvoiceRequest(
    val clientId: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal,
    @Serializable(with = LocalDateSerializer::class)
    val issueDate: LocalDate
) {
    fun toInvoice(invoiceId: Int): Invoice = Invoice(
        invoiceId = invoiceId,
        clientId = clientId,
        totalAmount = totalAmount,
        issueDate = issueDate
    )
}