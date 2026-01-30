package `in`.xroden.flockr.features.house.domain.usecase

import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.House
import javax.inject.Inject

/**
 * Use case for creating a house with validation and configuration.
 * Encapsulates the business logic for house creation.
 */
class CreateHouseUseCase @Inject constructor(
    private val houseRepository: HouseRepository
) {
    /**
     * Creates a new house with proper validation and default configuration.
     *
     * @param name House name
     * @param address Optional address
     * @param latitude Optional latitude coordinate
     * @param longitude Optional longitude coordinate
     * @param currencyCode Currency code (default USD)
     * @param dateFormat Date format preference (default dd/MM/yyyy)
     * @param firstDayOfWeek First day of week (0=Sunday, 1=Monday, default 1)
     * @param timezone Timezone (default UTC)
     * @return Result containing the created House or error
     */
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
        // Validation: Name should not be blank (already done by Validators)
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("House name cannot be blank"))
        }

        // Validation: If coordinates are provided, both should be present
        if ((latitude != null && longitude == null) || (latitude == null && longitude != null)) {
            return Result.failure(IllegalArgumentException("Both latitude and longitude must be provided"))
        }

        // Validation: Validate coordinate ranges
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
