package com.rayworks.droidweekly.repository

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.rayworks.droidweekly.di.KeyValueStorage
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef
import com.rayworks.droidweekly.repository.database.ArticleDao
import com.rayworks.droidweekly.repository.database.entity.Article
import com.rayworks.droidweekly.utils.jsonToObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/***
 * A repository as the data entry to load the article items.
 */
class ArticleRepository @Inject constructor(
    val articleDao: ArticleDao,
    val preferences: KeyValueStorage,
    val parser: WebContentParser
) : IArticleRepository {
    private var _refList: MutableLiveData<List<OldItemRef>> = MutableLiveData()
    private var _articleList: MutableLiveData<List<ArticleItem>> = MutableLiveData()

    private var articleLoaded = MutableLiveData<Boolean>()

    private var okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(
            TIMEOUT_IN_SECOND.toLong(),
            TimeUnit.SECONDS
        )
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AgentInterceptor(DROID_WEEKLY))
        .build()

    private val gson = Gson()

    override var refList: MutableLiveData<List<OldItemRef>>
        get() = _refList
        set(value) {
            _refList = value
        }
    override var articleList: MutableLiveData<List<ArticleItem>>
        get() = _articleList
        set(value) {
            _articleList = value
        }

    /***
     * Loads the latest issue.
     */
    override suspend fun loadData() {
        load(SITE_URL, ISSUE_ID_NONE)
    }

    /***
     * Loads the historical issue.
     */
    override suspend fun loadData(urlSubPath: String) {
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

    override suspend fun loadLocalArticlesBy(keyword: String): List<ArticleItem> {
        val result = withContext(Dispatchers.IO) {
            Timber.d(">>> loading local articles : ${Thread.currentThread().name}")
            articleDao.getArticleByKeyword("%${keyword}%").flatMap { article ->
                val articleItem = ArticleItem(
                    article.title, article.description, article.linkage)
                articleItem.imgFrameColor = article.imgFrameColor
                articleItem.imageUrl = article.imageUrl

                listOf(articleItem)
            }
        }
        return result
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

    private suspend fun load(url: String, issueId: Int) {
        withContext(Dispatchers.IO) {
            try {
                // notify the cached historical ref data first if found any
                val refItemStr = preferences.getString(REFERENCE_ISSUES_ID, "")
                if (refItemStr.isNotEmpty()) {
                    val list = gson.jsonToObject<List<OldItemRef>>(refItemStr)
                    if (!list.isNullOrEmpty()) {
                        Timber.i(">>> cached ref items found with size : %d", list.size)

                        withContext(Dispatchers.Main) {
                            _refList.value = list
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

            val data = response.body!!.string()
            val pair = parser.parse(data)

            val items = pair.first
            val itemRefs = pair.second

            // notify data changes
            if (itemRefs.isNotEmpty()) { // ISSUE_ID_NONE
                val latestId = itemRefs[0].issueId
                preferences.putInt(LATEST_ISSUE_ID, latestId)
                preferences.putString(REFERENCE_ISSUES_ID, gson.toJson(itemRefs))

                withContext(Dispatchers.Main) {
                    refList.value = itemRefs
                }

                val articles: List<Article> = articleDao.getArticlesByIssue(latestId)
                if (articles.isNotEmpty()) { // the latest issue content cache hits.
                    println(">>> cache hit for issue id : $latestId")
                    // articleList.value = getArticleModels(articles)
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
                Timber.w(">>> dump duplicate items in issue : %d", issueId)
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
