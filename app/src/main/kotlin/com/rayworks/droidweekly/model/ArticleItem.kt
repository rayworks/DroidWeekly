package com.rayworks.droidweekly.model

data class ArticleItem(var title: String = "", var description: String = "",
                       var linkage: String = "") {

    var imageUrl: String? = null

    var imgFrameColor: Int = 0
}
