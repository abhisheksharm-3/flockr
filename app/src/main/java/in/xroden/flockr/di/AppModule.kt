package `in`.xroden.flockr.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import `in`.xroden.flockr.core.network.NetworkMonitor
import `in`.xroden.flockr.core.network.RetryPolicy
import javax.inject.Singleton
import `in`.xroden.flockr.BuildConfig

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private const val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Auth) {
                defaultExternalAuthAction = io.github.jan.supabase.auth.ExternalAuthAction.CustomTabs()
            }
            install(Postgrest)
            install(Storage)
            install(Realtime)
            install(Functions)
        }
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }

    @Provides
    @Singleton
    fun provideRetryPolicy(): RetryPolicy {
        return RetryPolicy()
    }

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.30) // Use 30% of app memory for smoother scrolling
                    .strongReferencesEnabled(true) // Keep strong references for frequently accessed images
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100MB disk cache
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED) // Always use memory cache
            .diskCachePolicy(CachePolicy.ENABLED) // Always use disk cache
            .networkCachePolicy(CachePolicy.ENABLED) // Use network cache headers
            .respectCacheHeaders(false) // Ignore server cache headers for consistent caching
            .crossfade(150) // Fast crossfade animation
            .build()
    }
}
