package com.rayworks.droidweekly.di

import android.content.Context
import androidx.room.Room
import com.rayworks.droidweekly.repository.WebContentParser
import com.rayworks.droidweekly.repository.database.ArticleDao
import com.rayworks.droidweekly.repository.database.DATABASE_NAME
import com.rayworks.droidweekly.repository.database.IssueDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/***
 * Setup the providers of Application dependencies.
 */
@InstallIn(SingletonComponent::class)
@Module
object AppModule {
    /***
     * Provides a singleton [IssueDatabase] instance
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IssueDatabase {
        return Room
            .databaseBuilder(
                context.applicationContext,
                IssueDatabase::class.java,
                DATABASE_NAME,
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    /***
     * Provides a singleton [IssueDatabase] implementation
     */
    @Provides
    fun provideArticleDao(issueDatabase: IssueDatabase): ArticleDao {
        return issueDatabase.articleDao()
    }

    /***
     * Provides a [WebContentParser] instance
     */
    @Provides
    fun provideWebContentParser(): WebContentParser {
        return WebContentParser()
    }

    /***
     * Provides a [CoroutineDispatcher] instance
     */
    @Provides
    fun provideDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }
}
