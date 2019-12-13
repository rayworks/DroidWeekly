package com.rayworks.droidweekly.model

/***
 * The view data for an [Article]
 */
data class ArticleItem(var title: String = "", var description: String = "",
                       var linkage: String = "") {

    var imageUrl: String? = null

    var imgFrameColor: Int = 0
}
