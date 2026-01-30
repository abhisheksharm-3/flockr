package `in`.xroden.flockr.di

import `in`.xroden.flockr.utils.BiometricAuthManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BiometricEntryPoint {
    fun biometricAuthManager(): BiometricAuthManager
}
