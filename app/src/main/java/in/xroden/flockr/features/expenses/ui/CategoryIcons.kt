/** The icon each expense category is shown with. */
package `in`.xroden.flockr.features.expenses.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/** The icon for [category], or a generic one for a category the house typed itself. Settlements have no category. */
fun categoryIcon(category: String?): ImageVector = when (category) {
    null -> Icons.Rounded.SwapHoriz
    "Groceries" -> Icons.Rounded.LocalGroceryStore
    "Food & Dining" -> Icons.Rounded.Restaurant
    "Rent" -> Icons.Rounded.Home
    "Utilities" -> Icons.Rounded.Bolt
    "Internet" -> Icons.Rounded.Wifi
    "Transportation" -> Icons.Rounded.DirectionsCar
    "Entertainment" -> Icons.Rounded.Movie
    "Healthcare" -> Icons.Rounded.LocalHospital
    "Shopping" -> Icons.Rounded.ShoppingBag
    "Usage" -> Icons.Rounded.WaterDrop
    else -> Icons.Rounded.Category
}
