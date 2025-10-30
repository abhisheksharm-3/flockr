package `in`.xroden.flockr.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton
import `in`.xroden.flockr.BuildConfig

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // TODO: Replace with your actual Supabase URL and Anon Key
    private const val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private const val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        Log.d("AppModule", "========== Creating Supabase client ==========")
        Log.d("AppModule", "Supabase URL: $SUPABASE_URL")
        Log.d("AppModule", "Supabase Key length: ${SUPABASE_KEY.length}")

        return try {
            val client = createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_KEY
            ) {
                install(Auth) {
                    scheme = "flockr"
                    host = "login"
                }
                install(Postgrest)
                install(Storage)
                install(Realtime)
                install(Functions)
            }
            Log.d("AppModule", "✅ Supabase client created successfully")
            client
        } catch (e: Exception) {
            Log.e("AppModule", "❌ Error creating Supabase client", e)
            throw e
        }
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}

