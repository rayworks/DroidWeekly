package com.rayworks.droidweekly.repository

import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef
import com.rayworks.droidweekly.repository.exception.WebContentParsingException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.LinkedList

class WebContentParser {
    companion object {
        var logEnabled: Boolean = false
        fun debugPrint(message: Any?) {
            if (logEnabled) {
                println(message)
            }
        }

        fun parseColor(colorString: String): Int {
            if (colorString[0] == '#') { // Use a long to avoid rollovers on #ffXXXXXX
                var color: Long = colorString.substring(1).toLong(16)
                if (colorString.length == 7) { // Set the alpha value
                    color = color or -0x1000000
                } else require(colorString.length == 9) { "Unknown color" }
                return color.toInt()
            }

            return colorString.toInt()
        }
    }

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
                // build the issue menu items
                itemRefs.add(
                        0, OldItemRef("Issue $latestIssueId", "/issues/issue-$latestId", latestId)
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

    private fun parseSections(issue: Element): List<ArticleItem> {
        val sections = issue.getElementsByClass(SECTIONS)
        if (!sections.isEmpty()) {
            val tables =
                    sections[0].getElementsByTag(TABLE)
            debugPrint(">>> table size: " + tables.size)
            return parseArticleItems(tables)
        } else {
            throw WebContentParsingException("Parsing failure: sections not found")
        }
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
        return articleItems
    }
}
