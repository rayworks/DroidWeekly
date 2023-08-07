package com.rayworks.droidweekly.repository

import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef
import kotlinx.coroutines.flow.StateFlow

interface IArticleRepository {
    /***
     * The observable reference list of historical articles
     */
    var refList: StateFlow<List<OldItemRef>>

    /***
     * The observable list of [ArticleItem]s
     */
    var articleList: StateFlow<List<ArticleItem>>

    /***
     * Loads the latest article content
     */
    suspend fun loadData()

    /***
     * Loads article content with a known path
     */
    suspend fun loadData(urlSubPath: String)

    /***
     * Searches and loads the articles matched with the keyword
     */
    suspend fun loadLocalArticlesBy(keyword: String): List<ArticleItem>
}
