/** The categories an expense or per-diem item can be filed under. A house may also type its own. */
package `in`.xroden.flockr.features.expenses.model

object ExpenseCategories {
    val DEFAULT = listOf(
        "Groceries",
        "Food & Dining",
        "Rent",
        "Utilities",
        "Internet",
        "Transportation",
        "Entertainment",
        "Healthcare",
        "Shopping",
        "Other"
    )
}

object PerDiemCategories {
    val DEFAULT = listOf("Groceries", "Food & Dining", "Utilities", "Other")
}
