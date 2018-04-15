package com.rayworks.droidweekly.repository.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.rayworks.droidweekly.repository.database.entity.Article;

import java.util.List;

@Dao
public interface ArticleDao {
    @Query("SELECT * FROM article WHERE issue_id IS :issue ORDER BY `order`")
    List<Article> getArticlesByIssue(int issue);

    @Query("SELECT * from article WHERE description LIKE :key")
    List<Article> getArticleByKeyword(String key);

    @Insert
    void insertAll(List<Article> articles);

    @Update
    void update(Article article);

    @Delete
    void delete(Article article);
}
