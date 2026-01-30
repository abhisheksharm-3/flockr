package `in`.xroden.flockr.features.house.data

import `in`.xroden.flockr.data.dto.HouseInvitationInsert
import `in`.xroden.flockr.data.enums.InvitationStatus
import `in`.xroden.flockr.features.house.model.InvitationWithHouse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HouseInvitationRepositoryTest {

    private lateinit var supabase: SupabaseClient
    private lateinit var postgrest: Postgrest
    private lateinit var houseRepository: HouseRepository
    private lateinit var repository: HouseInvitationRepository

    @Before
    fun setup() {
        supabase = mockk()
        postgrest = mockk()
        houseRepository = mockk()
        
        mockkStatic("io.github.jan.supabase.postgrest.PostgrestKt")
        every { supabase.postgrest } returns postgrest

        repository = HouseInvitationRepository(supabase, houseRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getPendingInvitations returns list of invitations`() = runTest {
        val mockInvitations = listOf(
            InvitationWithHouse(
                id = "1",
                houseId = "h1",
                inviterId = "user1",
                inviteeEmail = "test@test.com",
                status = InvitationStatus.PENDING,
                createdAt = Instant.fromEpochMilliseconds(0),
                houseName = "Test House"
            )
        )

        coEvery {
            postgrest.rpc(
                function = "get_user_invitations",
                parameters = any()
            ).decodeList<InvitationWithHouse>()
        } returns mockInvitations

        val result = repository.getPendingInvitations()

        assertEquals(true, result.isSuccess)
        assertEquals(mockInvitations, result.getOrNull())
        
        coVerify { 
            postgrest.rpc(
                function = "get_user_invitations", 
                parameters = any()
            ) 
        }
    }

    @Test
    fun `inviteMember calls correct RPC`() = runTest {
        val houseId = "h1"
        val testEmail = "new@test.com"

        coEvery {
            postgrest.rpc(
                function = "invite_user_to_house",
                parameters = any<HouseInvitationInsert>()
            )
        } returns mockk()

        val result = repository.inviteMember(houseId, testEmail)

        assertEquals(true, result.isSuccess)

        coVerify {
            postgrest.rpc(
                function = "invite_user_to_house",
                parameters = match<HouseInvitationInsert> { 
                    it.houseId == houseId && it.inviteeEmail == testEmail
                }
            )
        }
    }
}
