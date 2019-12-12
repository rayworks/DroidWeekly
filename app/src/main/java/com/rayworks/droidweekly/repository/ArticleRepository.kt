package com.rayworks.droidweekly.repository

import android.content.SharedPreferences
import android.graphics.Color
import androidx.lifecycle.MutableLiveData
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException
import java.util.LinkedList
import java.util.concurrent.TimeUnit

class ArticleRepository(val articleDao: ArticleDao, val preferences: SharedPreferences) {
    var refList: MutableLiveData<List<OldItemRef>> = MutableLiveData()

    var articleList: MutableLiveData<List<ArticleItem>> = MutableLiveData()

    var articleLoaded = MutableLiveData<Boolean>()

    companion object {
        val PAST_ISSUES = "past-issues"
        val LATEST_ISSUE = "latest-issue"
        val ISSUE_HEADER = "issue-header"
        val SECTIONS = "sections"
        val TABLE = "table"
        val ISSUE_INFO = "issue_info"
        val LATEST_ISSUE_ID = "latest_issue_id"
        private val SITE_URL = "http://androidweekly.net" // /issues/issue-302

        private val DROID_WEEKLY = "DroidWeekly"
        private val TIMEOUT_IN_SECOND = 10
        val DATABASE_NAME = "my_db"
        private val ISSUE_ID_NONE = -1
    }

    private var okHttpClient: OkHttpClient

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

    suspend fun loadData() {
        load(SITE_URL, ISSUE_ID_NONE)
    }

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

    suspend fun load(url: String, issueId: Int) {
        withContext(Dispatchers.IO) {
            try {
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
            } catch (exp: Exception) {
                exp.printStackTrace()

                setDataLoaded(false)
            }

        }
    }

    private suspend fun fetchRemote(url: String, issueId: Int) {
        try {
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()

            processResponse(response.body()!!.string(), issueId)

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

    private suspend fun processResponse(data: String, issueId: Int) {
        val doc = Jsoup.parse(data)
        val itemRefs: MutableList<OldItemRef> =
            LinkedList()
        val pastIssues =
            doc.getElementsByClass(PAST_ISSUES)
        if (!pastIssues.isEmpty()) { // contained only in the request for the latest issue
            val passIssueGrp = pastIssues[0]
            val tags = passIssueGrp.getElementsByTag("ul")
            val ulTag = tags[0]
            val liTags = ulTag.getElementsByTag("li")
            val cnt = liTags.size
            for (i in 0 until cnt) {
                val li = liTags[i]
                val elements = li.getElementsByTag("a")
                val refItem = elements[0]
                val oldItemRef = OldItemRef(refItem.text(), refItem.attr("href"))
                println("<<< old issue: " + refItem.text())
                if (oldItemRef.relativePath.contains("issue-")) {
                    itemRefs.add(oldItemRef)
                }
            }
        }
        val latestIssues =
            doc.getElementsByClass(LATEST_ISSUE)
        val currentIssues = doc.getElementsByClass("issue")
        if (!latestIssues.isEmpty()) {
            var latestId = 0
            val issue = latestIssues[0]
            val headers =
                issue.getElementsByClass(ISSUE_HEADER)
            if (!headers.isEmpty()) {
                val header = headers[0]
                // #308
                val latestIssueId = header.getElementsByClass("clearfix")[0]
                    .getElementsByTag("span")
                    .text()
                if (latestIssueId.startsWith("#")) {
                    latestId = latestIssueId.substring(1).toInt()
                }
                // build the issue menu items
                itemRefs.add(
                    0, OldItemRef("Issue $latestIssueId", "issues/issue-$latestId")
                )
                preferences.edit().putInt(LATEST_ISSUE_ID, latestId).apply()

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
            val sections = issue.getElementsByClass(SECTIONS)
            if (!sections.isEmpty()) {
                val tables =
                    sections[0].getElementsByTag(TABLE)
                println(">>> table size: " + tables.size)
                val articleItems = parseArticleItems(tables)
                //articleList.value = articleItems
                updateList(articleItems)

                val entities = getArticleEntities(latestId, articleItems)
                articleDao.insertAll(entities)
            } else {
                throw IOException("Parsing failure: sections not found")
            }
        } else if (!currentIssues.isEmpty()) {
            val items = parseArticleItemsForIssue(doc)
            //articleList.value = items
            updateList(items)

            val entities = getArticleEntities(issueId, items)
            articleDao.insertAll(entities)
        } else {
            throw IOException("Parsing failure: latest-issue not found")
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

    private fun parseArticleItemsForIssue(doc: Document): List<ArticleItem> {
        val issues = doc.getElementsByClass("issue")
        val tables = issues[0].getElementsByTag("table")
        return parseArticleItems(tables)
    }

    private fun parseArticleItems(tables: Elements): List<ArticleItem> {
        val articleItems: MutableList<ArticleItem> =
            LinkedList()
        for (i in tables.indices) {
            val articleItem = ArticleItem()
            val element = tables[i]
            val imageElems = element.getElementsByTag("img")
            if (!imageElems.isEmpty()) {
                val imageElem = imageElems[0]
                if (imageElem != null) {
                    articleItem.imageUrl = imageElem.attr("src")
                    val style = imageElem.attr("style")
                    val begPos = style.indexOf("border")
                    if (begPos >= 0) {
                        val startPos = style.indexOf('#', begPos)
                        val endPos = style.indexOf(";", startPos)
                        if (startPos >= 0 && endPos >= 0) {
                            articleItem.imgFrameColor =
                                Color.parseColor(style.substring(startPos, endPos))
                        }
                    }
                }
            }
            val elementsByClass =
                element.getElementsByClass("article-headline")
            if (!elementsByClass.isEmpty()) {
                val headline = elementsByClass[0]
                if (headline != null) {
                    val text = headline.text()
                    println(">>> HEAD_LINE: $text")
                    val href = headline.attr("href")
                    println(">>> HEAD_URL : $href")
                    articleItem.title = text
                    articleItem.linkage = href
                }
            }
            val paragraphs = element.getElementsByTag("p")
            if (!paragraphs.isEmpty()) {
                val description = paragraphs[0].text()
                articleItem.description = description
                println(">>> HEAD_DESC: $description")
            }
            var title = element.selectFirst("h2")
            if (title != null) {
                val text = title.text()
                println(">>>$text")
                articleItem.title = text
            } else { // tag Sponsored
                title = element.selectFirst("h5")
                if (title != null) {
                    articleItem.title = title.text()
                }
            }
            articleItems.add(articleItem)
        }
        return articleItems
    }
}
