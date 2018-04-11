package com.rayworks.droidweekly.repository.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.rayworks.droidweekly.repository.database.entity.Article;

@Database(
    entities = {Article.class},
    version = 1,
    exportSchema = false
)
public abstract class IssueDatabase extends RoomDatabase {
    public abstract ArticleDao articleDao();
}
