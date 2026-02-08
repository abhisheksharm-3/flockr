package `in`.xroden.flockr.data.dto.perdiem

import `in`.xroden.flockr.data.serialization.LocalDateSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Parameters for getting per diem bill. */
@Serializable
data class GetPerDiemBillParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month") val month: String
)

/** Parameters for getting per diem bill by member. */
@Serializable
data class PerDiemBillByMemberParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month") val month: String
)

/** Parameters for getting per diem entries. */
@Serializable
data class PerDiemEntriesParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month")
    @Serializable(with = LocalDateSerializer::class)
    val month: LocalDate? = null
)

/** Parameters for getting per diem config. */
@Serializable
data class PerDiemConfigParams(
    @SerialName("p_house_id") val houseId: String
)
