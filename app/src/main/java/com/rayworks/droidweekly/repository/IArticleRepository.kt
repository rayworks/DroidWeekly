package com.rayworks.droidweekly.repository

import androidx.lifecycle.MutableLiveData
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef

interface IArticleRepository {
    /***
     * The observable reference list of historical articles
     */
    var refList: MutableLiveData<List<OldItemRef>>

    /***
     * The observable list of [ArticleItem]s
     */
    var articleList: MutableLiveData<List<ArticleItem>>

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
    suspend fun loadLocalArticlesBy(keyword: String) : List<ArticleItem>
}
