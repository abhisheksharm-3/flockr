package `in`.xroden.flockr.features.expenses.domain.usecase

import `in`.xroden.flockr.features.expenses.data.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Use case for settling balances between house members.
 * Encapsulates the complex business logic for creating settlement transactions.
 */
class SettleBalanceUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Settles a balance between two members by creating a settlement transaction.
     *
     * @param houseId The house where the settlement occurs
     * @param payerId The user who is paying
     * @param payeeId The user who is receiving payment
     * @param amount The amount being settled
     * @param payerName Name of the payer for transaction description
     * @param payeeName Name of the payee for transaction description
     * @param notes Optional notes for the settlement
     * @param date The date of the settlement
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        houseId: String,
        payerId: String,
        payeeId: String,
        amount: BigDecimal,
        payerName: String,
        payeeName: String,
        notes: String?,
        date: kotlinx.datetime.LocalDate
    ): Result<Unit> {
        val title = "$payerName settled with $payeeName"

        return transactionRepository.settleBalance(
            houseId = houseId,
            payerId = payerId,
            payeeId = payeeId,
            amount = amount,
            date = date,
            name = title,
            notes = notes
        )
    }
}
