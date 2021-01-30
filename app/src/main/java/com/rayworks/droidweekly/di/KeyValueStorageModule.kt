package com.rayworks.droidweekly.di

import com.rayworks.droidweekly.repository.AndroidKVStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
abstract class KeyValueStorageModule {
    @Singleton
    @Binds
    abstract fun bindKvStorage(impl: AndroidKVStorage): KeyValueStorage
}