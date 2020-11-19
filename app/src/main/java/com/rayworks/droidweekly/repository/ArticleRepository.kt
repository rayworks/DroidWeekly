package com.rayworks.droidweekly.repository

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.rayworks.droidweekly.utils.jsonToObject
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef
import com.rayworks.droidweekly.repository.database.ArticleDao
import com.rayworks.droidweekly.repository.database.entity.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

/***
 * A repository as the data entry to load the article items.
 */
class ArticleRepository(val articleDao: ArticleDao, val preferences: SharedPreferences, val parser: WebContentParser) {
    var refList: MutableLiveData<List<OldItemRef>> = MutableLiveData()

    var articleList: MutableLiveData<List<ArticleItem>> = MutableLiveData()

    var articleLoaded = MutableLiveData<Boolean>()

    private var okHttpClient: OkHttpClient

    private val gson = Gson()

    init {

        okHttpClient = OkHttpClient.Builder()
                .readTimeout(
                        TIMEOUT_IN_SECOND.toLong(),
                        TimeUnit.SECONDS
                )
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(AgentInterceptor(DROID_WEEKLY))
                .build()
    }

    /***
     * Loads the latest issue.
     */
    suspend fun loadData() {
        load(SITE_URL, ISSUE_ID_NONE)
    }

    /***
     * Loads the historical issue.
     */
    suspend fun loadData(urlSubPath: String) {
        var id = ISSUE_ID_NONE
        // format like : issues/issue-302
        val segments = urlSubPath.split("-").toTypedArray()
        if (segments.isNotEmpty()) {
            val index = segments.size - 1
            id = segments[index].toInt()
            println(">>> found issue id : $id")
        }
        load(SITE_URL + urlSubPath, id)
    }

    private suspend fun updateList(list: List<ArticleItem>) {
        withContext(Dispatchers.Main) {
            articleList.value = list

            println(">>> data : ${list.size} items loaded")
        }
    }

    private suspend fun setDataLoaded(loaded: Boolean) {
        withContext(Dispatchers.Main) {
            articleLoaded.value = loaded
        }
    }

    /***
     * Loads the issue content.
     */
    suspend fun load(url: String, issueId: Int) {
        withContext(Dispatchers.IO) {
            try {
                // notify the cached historical ref data first if found any
                val refItemStr = preferences.getString(REFERENCE_ISSUES_ID, "")
                if (!refItemStr.isNullOrEmpty()) {
                    val list = gson.jsonToObject<List<OldItemRef>>(refItemStr)
                    if (!list.isNullOrEmpty()) {
                        Timber.i(">>> cached ref items found with size : %d", list.size)
                        GlobalScope.launch {
                            withContext(Dispatchers.Main) { refList.value = list }
                        }
                    }
                }

                if (issueId > 0) {
                    val articlesByIssue = articleDao.getArticlesByIssue(issueId)
                    if (articlesByIssue.isNullOrEmpty()) {
                        fetchRemote(url, issueId)
                    } else {
                        updateList(getArticleModels(articlesByIssue))
                    }
                } else {
                    fetchRemote(url, issueId)
                }

                setDataLoaded(true)
            } catch (exp: IOException) {

                setDataLoaded(false)
                throw exp
            }

        }
    }

    private suspend fun fetchRemote(url: String, issueId: Int) {
        try {
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()

            val data = response.body()!!.string();
            val pair = parser.parse(data)

            val items = pair.first
            val itemRefs = pair.second

            // notify data changes
            if (itemRefs.isNotEmpty()) { // ISSUE_ID_NONE
                val latestId = itemRefs[0].issueId
                preferences.edit().putInt(LATEST_ISSUE_ID, latestId).apply()
                preferences.edit().putString(REFERENCE_ISSUES_ID, gson.toJson(itemRefs)).apply()

                GlobalScope.launch {
                    withContext(Dispatchers.Main) { refList.value = itemRefs }
                }

                val articles: List<Article> = articleDao.getArticlesByIssue(latestId)
                if (articles.isNotEmpty()) { // the latest issue content cache hits.
                    println(">>> cache hit for issue id : $latestId")
                    //articleList.value = getArticleModels(articles)
                    updateList(getArticleModels(articles))
                    return
                }
            }

            updateList(items)

            val list = articleDao.getArticlesByIssue(issueId)
            if (list.isNullOrEmpty()) {
                // add the local cache
                val entities = getArticleEntities(issueId, items)
                articleDao.insertAll(entities)
            } else {
                Timber.w(">>> dump duplicate items in issue : %d", issueId);
            }

        } catch (exception: IOException) {
            exception.printStackTrace()

            val lastId = preferences.getInt(LATEST_ISSUE_ID, 0)
            if (lastId > 0 && issueId == ISSUE_ID_NONE) {
                val articleList: List<Article> =
                        articleDao.getArticlesByIssue(lastId)

                updateList(getArticleModels(articleList))

                return
            }

            throw exception
        }
    }

    private fun getArticleModels(articles: List<Article>): List<ArticleItem> {
        val items: MutableList<ArticleItem> = LinkedList()
        for (article in articles) {
            val articleItem = ArticleItem(
                    article.title, article.description, article.linkage
            )
            articleItem.imgFrameColor = article.imgFrameColor
            articleItem.imageUrl = article.imageUrl
            items.add(articleItem)
        }
        return items
    }

    private fun getArticleEntities(
            issueId: Int,
            items: List<ArticleItem>
    ): List<Article> {
        val entities: MutableList<Article> = LinkedList()
        var index = 0
        for (item in items) {
            ++index
            val article = Article(
                    title = item.title,
                    description = item.description,
                    imageUrl = item.imageUrl ?: "",
                    imgFrameColor = item.imgFrameColor,
                    linkage = item.linkage,
                    order = index,
                    issueId = issueId
            )
            entities.add(article)
        }
        return entities
    }
}
