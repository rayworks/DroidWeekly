package com.rayworks.droidweekly.repository.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rayworks.droidweekly.repository.database.entity.Article

/***
 * The [Article] DB
 */
@Database(entities = [Article::class], version = 1, exportSchema = false)
abstract class IssueDatabase : RoomDatabase() {
    /***
     * The abstract interface to access the article data
     */
    abstract fun articleDao(): ArticleDao
}

private lateinit var INSTANCE: IssueDatabase

const val DATABASE_NAME = "my_db"
