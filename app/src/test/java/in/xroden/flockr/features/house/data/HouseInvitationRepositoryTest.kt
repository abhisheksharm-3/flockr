package `in`.xroden.flockr.features.house.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [HouseInvitationRepository] focused on authorization/guard behavior, which is
 * verifiable without mocking Supabase's reified rpc/from builders. Happy-path RPC calls are
 * exercised by integration tests against a real Supabase instance.
 */
class HouseInvitationRepositoryTest {

    private lateinit var supabase: SupabaseClient
    private lateinit var postgrest: Postgrest
    private lateinit var houseRepository: IHouseRepository
    private lateinit var repository: HouseInvitationRepository

    @Before
    fun setup() {
        supabase = mockk()
        postgrest = mockk()
        houseRepository = mockk()

        mockkStatic("io.github.jan.supabase.postgrest.PostgrestKt")
        mockkStatic("io.github.jan.supabase.auth.AuthKt")
        every { supabase.postgrest } returns postgrest

        repository = HouseInvitationRepository(supabase, houseRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getPendingInvitations returns empty when signed out`() = runTest {
        every { supabase.auth.currentUserOrNull() } returns null

        val result = repository.getPendingInvitations()

        assertEquals(true, result.isSuccess)
        assertEquals(emptyList<Any>(), result.getOrNull())
    }

    @Test
    fun `inviteMember fails when not authenticated`() = runTest {
        every { supabase.auth.currentUserOrNull() } returns null

        val result = repository.inviteMember("h1", "new@test.com")

        assertEquals(true, result.isFailure)
    }

    @Test
    fun `inviteMember fails when the house does not exist`() = runTest {
        every { supabase.auth.currentUserOrNull() } returns mockk { every { id } returns "user1" }
        coEvery { houseRepository.getHouseById("h1") } returns Result.failure(RuntimeException("not found"))

        val result = repository.inviteMember("h1", "new@test.com")

        assertEquals(true, result.isFailure)
    }
}
