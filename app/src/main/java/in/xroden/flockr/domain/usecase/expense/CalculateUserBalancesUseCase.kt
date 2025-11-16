package `in`.xroden.flockr.domain.usecase.expense

import `in`.xroden.flockr.features.expenses.model.UserBalance
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Use case to calculate balances between users
 * Determines who owes whom and how much
 */
class CalculateUserBalancesUseCase @Inject constructor() {

    /**
     * Calculate simplified balances from raw balance data
     * 
     * @param balances Raw balance data from database
     * @return Simplified list of balances (positive = owed to them, negative = they owe)
     */
    operator fun invoke(balances: List<UserBalance>): List<UserBalance> {
        // Filter out zero balances and amounts less than 1 cent
        return balances.filter { balance ->
            balance.balance != BigDecimal.ZERO && 
            balance.balance.abs() >= BigDecimal("0.01")
        }.sortedByDescending { it.balance.abs() }
    }

    /**
     * Calculate settlement transactions to minimize number of transactions
     * 
     * @param balances List of user balances
     * @return List of suggested settlement transactions
     */
    fun calculateSettlementTransactions(balances: List<UserBalance>): List<SettlementTransaction> {
        val creditors = balances.filter { it.balance > BigDecimal.ZERO }
            .sortedByDescending { it.balance }
            .toMutableList()
        
        val debtors = balances.filter { it.balance < BigDecimal.ZERO }
            .sortedBy { it.balance }
            .toMutableList()
        
        val transactions = mutableListOf<SettlementTransaction>()
        
        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val creditor = creditors.first()
            val debtor = debtors.first()
            
            val amount = minOf(creditor.balance, debtor.balance.abs())
            
            transactions.add(
                SettlementTransaction(
                    payerId = debtor.userId,
                    payerName = debtor.fullName ?: "Unknown",
                    payeeId = creditor.userId,
                    payeeName = creditor.fullName ?: "Unknown",
                    amount = amount
                )
            )
            
            // Update balances
            creditors[0] = creditor.copy(balance = creditor.balance - amount)
            debtors[0] = debtor.copy(balance = debtor.balance + amount)
            
            // Remove settled balances
            if (creditors[0].balance < BigDecimal("0.01")) {
                creditors.removeAt(0)
            }
            if (debtors[0].balance.abs() < BigDecimal("0.01")) {
                debtors.removeAt(0)
            }
        }
        
        return transactions
    }

    data class SettlementTransaction(
        val payerId: String,
        val payerName: String,
        val payeeId: String,
        val payeeName: String,
        val amount: BigDecimal
    )
}


