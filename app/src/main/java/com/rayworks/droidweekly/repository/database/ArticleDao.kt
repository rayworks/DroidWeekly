package com.rayworks.droidweekly.repository.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rayworks.droidweekly.repository.database.entity.Article

/***
 * The DB access interfaces.
 */
@Dao
interface ArticleDao {
    /***
     * Retrieves all the [Article] records with a specific [issue] id
     */
    @Query("SELECT * FROM article WHERE issue_id IS :issue ORDER BY `order`")
    fun getArticlesByIssue(issue: Int): List<Article>

    /***
     * Does a fuzzy search with provided [key]
     */
    @Query("SELECT * from article WHERE description LIKE :key OR title LIKE :key")
    fun getArticleByKeyword(key: String): List<Article>

    /***
     * Inserts a bunch of records
     */
    @Insert
    fun insertAll(articles: List<Article>)

    /***
     * Updates a records
     */
    @Update
    fun update(article: Article)

    /***
     * Deletes the record
     */
    @Delete
    fun delete(article: Article)
}