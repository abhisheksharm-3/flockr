package `in`.xroden.flockr.features.chores.domain.usecase

import `in`.xroden.flockr.features.chores.data.IChoreRepository
import javax.inject.Inject

/**
 * Use case for completing chores.
 * Handles the business logic of marking a chore as complete.
 */
class CompleteChoreUseCase @Inject constructor(
    private val choreRepository: IChoreRepository
) {
    /**
     * Marks a chore as complete.
     *
     * @param choreId The ID of the chore to complete
     * @param houseId The house ID (for refreshing the list)
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        choreId: String,
        houseId: String
    ): Result<Unit> {
        return choreRepository.completeChore(choreId, houseId)
    }
}
