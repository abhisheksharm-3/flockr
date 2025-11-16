package `in`.xroden.flockr.data.dto

import `in`.xroden.flockr.data.serialization.BigDecimalSerializer
import `in`.xroden.flockr.data.serialization.LocalDateSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class PerDiemConfigInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("item_name")
    val itemName: String,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal,
    val category: String,
    val unit: String = "unit"
)

@Serializable
data class PerDiemConfigUpdate(
    @SerialName("item_name")
    val itemName: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal? = null,
    val category: String? = null,
    val unit: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null
)

@Serializable
data class PerDiemEntryInsert(
    @SerialName("config_id")
    val configId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    @SerialName("added_by")
    val addedBy: String,
    val notes: String? = null
)

@Serializable
data class PerDiemEntryUpdate(
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal? = null,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate? = null,
    val notes: String? = null
)


