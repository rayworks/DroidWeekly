package com.rayworks.droidweekly.repository.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rayworks.droidweekly.repository.database.entity.Article

@Database(entities = [Article::class], version = 1, exportSchema = false)
abstract class IssueDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao?
}

private lateinit var INSTANCE: IssueDatabase

/**
 * Instantiate a database from a context.
 */
fun getDatabase(context: Context): IssueDatabase {
    synchronized(IssueDatabase::class) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room
                .databaseBuilder(
                    context.applicationContext,
                    IssueDatabase::class.java,
                    "my_db"
                )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
    return INSTANCE
}