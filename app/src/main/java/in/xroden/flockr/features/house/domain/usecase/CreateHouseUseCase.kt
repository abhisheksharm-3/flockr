package `in`.xroden.flockr.features.house.domain.usecase

import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.House
import javax.inject.Inject

/** Use case for creating a house with validation and configuration. */
class CreateHouseUseCase @Inject constructor(
    private val houseRepository: IHouseRepository
) {
    suspend operator fun invoke(
        name: String,
        address: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        currencyCode: String = "USD",
        dateFormat: String = "dd/MM/yyyy",
        firstDayOfWeek: Int = 1,
        timezone: String = "UTC"
    ): Result<House> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("House name cannot be blank"))
        }

        if ((latitude != null && longitude == null) || (latitude == null && longitude != null)) {
            return Result.failure(IllegalArgumentException("Both latitude and longitude must be provided"))
        }

        if (latitude != null && longitude != null) {
            if (latitude !in -90.0..90.0) {
                return Result.failure(IllegalArgumentException("Latitude must be between -90 and 90"))
            }
            if (longitude !in -180.0..180.0) {
                return Result.failure(IllegalArgumentException("Longitude must be between -180 and 180"))
            }
        }

        return houseRepository.createHouse(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            currencyCode = currencyCode,
            dateFormat = dateFormat,
            firstDayOfWeek = firstDayOfWeek,
            timezone = timezone
        )
    }
}
