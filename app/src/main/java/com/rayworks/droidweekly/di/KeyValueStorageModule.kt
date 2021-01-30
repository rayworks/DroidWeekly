package com.rayworks.droidweekly.di

import com.rayworks.droidweekly.repository.AndroidKVStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

/***
 * The KV storage module binds the [KeyValueStorage] interface to one of implementations : [AndroidKVStorage]
 */
@InstallIn(ApplicationComponent::class)
@Module
abstract class KeyValueStorageModule {
    /***
     * The method does the action binding
     */
    @Singleton
    @Binds
    abstract fun bindKvStorage(impl: AndroidKVStorage): KeyValueStorage
}