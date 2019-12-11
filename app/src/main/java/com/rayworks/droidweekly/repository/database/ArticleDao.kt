package com.rayworks.droidweekly.repository.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rayworks.droidweekly.repository.database.entity.Article

@Dao
interface ArticleDao {
    @Query("SELECT * FROM article WHERE issue_id IS :issue ORDER BY `order`")
    fun getArticlesByIssue(issue: Int): List<Article>

    @Query("SELECT * from article WHERE description LIKE :key")
    fun getArticleByKeyword(key: String): List<Article>

    @Insert
    fun insertAll(articles: List<Article>)

    @Update
    fun update(article: Article)

    @Delete
    fun delete(article: Article)
}