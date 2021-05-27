package com.rayworks.droidweekly.di

import com.rayworks.droidweekly.repository.AndroidKVStorage
import com.rayworks.droidweekly.repository.ArticleRepository
import com.rayworks.droidweekly.repository.IArticleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/***
 * The KV storage module binds the [KeyValueStorage] interface to one of implementations : [AndroidKVStorage]
 */
@InstallIn(SingletonComponent::class)
@Module
abstract class BindingModule {
    /***
     * The method does the action binding
     */
    @Singleton
    @Binds
    abstract fun bindKvStorage(impl: AndroidKVStorage): KeyValueStorage


    @Singleton
    @Binds
    abstract fun bindRepo(repo: ArticleRepository) : IArticleRepository
}
