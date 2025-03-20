package com.rayworks.droidweekly.repository

import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef
import com.rayworks.droidweekly.repository.exception.WebContentParsingException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.LinkedList

/***
 * The parser used for extracting structured info from a web page.
 */
class WebContentParser {
    companion object {
        var logEnabled: Boolean = false

        /***
         * Local debug method for printing logs.
         */
        fun debugPrint(message: Any?) {
            if (logEnabled) {
                println(message)
            }
        }

        /***
         * Maps a string color value to an Int
         */
        fun parseColor(colorString: String): Int {
            if (colorString[0] == '#') { // Use a long to avoid rollovers on #ffXXXXXX
                var color: Long = colorString.substring(1).toLong(16)
                if (colorString.length == 7) { // Set the alpha value
                    color = color or -0x1000000
                } else {
                    require(colorString.length == 9) { "Unknown color" }
                }
                return color.toInt()
            }

            return colorString.toInt()
        }
    }

    /***
     * Parses the article items and related references
     */
    fun parse(data: String): Pair<List<ArticleItem>, List<OldItemRef>> {
        val doc = Jsoup.parse(data)
        val itemRefs: MutableList<OldItemRef> = LinkedList()
        val items: List<ArticleItem>

        val pastIssues = doc.getElementsByClass(PAST_ISSUES)
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
                debugPrint("<<< old issue: " + refItem.text())
                if (oldItemRef.relativePath.contains("issue-")) {
                    itemRefs.add(oldItemRef)
                }
            }
        }
        val latestIssues =
            doc.getElementsByClass(LATEST_ISSUE)
        val currentIssues = doc.getElementsByClass("issue")
        if (!latestIssues.isEmpty()) { // the latest issue content
            val issue = latestIssues[0]
            var latestId = 0
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

                if (latestId == 0) {
                    latestId = extractLatestIssueIdFromBadge(issue)
                }
                // build the issue menu items
                itemRefs.add(
                    0,
                    OldItemRef("Issue #$latestId", "/issues/issue-$latestId", latestId),
                )
            }

            items = parseSections(issue)
        } else if (currentIssues.isNotEmpty()) {
            items = parseArticleItemsForIssue(doc)
        } else {
            throw WebContentParsingException("NO issue data found")
        }

        return Pair(items, itemRefs)
    }

    private fun extractLatestIssueIdFromBadge(issue: Element): Int {
        var id = 0
        val badges = issue.getElementsByClass("code-badge")
        if (badges.isNotEmpty()) {
            val badge = badges[0]
            val imgTag = badge.getElementsByTag("img").getOrNull(0)
            if (imgTag != null) {
                val src = imgTag.attr("src")
                // eg. "https://androidweekly.net/issues/issue-666/badge"
                val issuePrefix = "issue-"
                val pos = src.indexOf(issuePrefix)
                if (pos >= 0) {
                    val end = src.indexOf("/", pos)
                    if (end >= 0) {
                        id = src.substring(pos + issuePrefix.length, end).toInt()
                    }
                }
            }
        }
        return id
    }

    private fun parseSections(issue: Element): List<ArticleItem> {
        val sections = issue.getElementsByClass(SECTIONS)
        if (!sections.isEmpty()) {
            val newEntries =
                sections[0].getElementsByClass("text-container galileo-ap-content-editor")
            val tables =
                if (newEntries.isEmpty()) sections[0].getElementsByTag(TABLE) else newEntries

            debugPrint(">>> table size: " + tables.size)
            return parseArticleItems(sections[0], tables, newEntries.isNotEmpty())
        } else {
            throw WebContentParsingException("Parsing failure: sections not found")
        }
    }

    private fun parseArticleItemsForIssue(doc: Document): List<ArticleItem> {
        val issues = doc.getElementsByClass("issue")
        val newEntries = issues[0].getElementsByClass("text-container galileo-ap-content-editor")
        val tables = if (newEntries.isEmpty()) issues[0].getElementsByTag(TABLE) else newEntries

        return parseArticleItems(issues[0], tables, newEntries.isNotEmpty())
    }

    private fun parseArticleItems(
        section: Element,
        tables: Elements,
        newEntryStyle: Boolean
    ): List<ArticleItem> {
        val articleItems: MutableList<ArticleItem> = LinkedList()

        if (newEntryStyle) {
            val map = mutableMapOf<String, String>()
            val mediaSrcList = section.getElementsByClass("publish-container")
            for (e in mediaSrcList) {
                val href = e.getElementsByTag("a")[0]
                if (!href.attr("href").isNullOrEmpty()) {
                    val imgs = href.getElementsByTag("img")
                    if (imgs.isNotEmpty()) {
                        val imageUrl = imgs[0].attr("src")
                        if (imageUrl.isNotEmpty()) {
                            map[href.attr("href")] = imageUrl
                        }
                    }
                }
            }
            parseArticles(tables, articleItems, map)
        } else {
            parseLegacyArticles(tables, articleItems)
        }

        return articleItems
    }

    private fun parseArticles(
        tables: Elements,
        articleItems: MutableList<ArticleItem>,
        map: MutableMap<String, String>,
    ) {
        for (elem in tables) {
            val articleItem = ArticleItem()

            val nodes = elem.child(0).children()
            if (nodes.size == 2) {
                val href = nodes[0].getElementsByTag("a")[0]
                articleItem.linkage = href.attr("href")
                articleItem.title = href.text()
                if (map.containsKey(articleItem.linkage)) {
                    articleItem.imageUrl = map[articleItem.linkage]
                }

                val style = href.attr("style")
                val startPos = style.indexOf('#', 0)
                val endPos = style.indexOf(";", startPos)
                if (startPos >= 0 && endPos >= 0) {
                    articleItem.imgFrameColor =
                        parseColor(style.substring(startPos, endPos))
                }

                articleItem.description = nodes[1].text()

                articleItems.add(articleItem)
            } else if (nodes.size == 1) {
                nodes[0].getElementsByTag("span").let {
                    if (it.size > 0) {
                        articleItem.title = it[0].text()
                        articleItems.add(articleItem)
                    }
                }
            }
        }
    }

    private fun parseLegacyArticles(
        tables: Elements,
        articleItems: MutableList<ArticleItem>,
    ) {
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
                                parseColor(style.substring(startPos, endPos))
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
                    debugPrint(">>> HEAD_LINE: $text")
                    val href = headline.attr("href")
                    debugPrint(">>> HEAD_URL : $href")
                    articleItem.title = text
                    articleItem.linkage = href
                }
            }
            val paragraphs = element.getElementsByTag("p")
            if (!paragraphs.isEmpty()) {
                val description = paragraphs[0].text()
                articleItem.description = description
                debugPrint(">>> HEAD_DESC: $description")
            }
            var title = element.selectFirst("h2")
            if (title != null) {
                val text = title.text()
                debugPrint(">>>$text")
                articleItem.title = text
            } else { // tag Sponsored
                title = element.selectFirst("h5")
                if (title != null) {
                    articleItem.title = title.text()
                }
            }
            articleItems.add(articleItem)
        }
    }
}
