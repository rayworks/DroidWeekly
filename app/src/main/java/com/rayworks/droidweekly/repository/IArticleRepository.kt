package com.rayworks.droidweekly.repository

import androidx.lifecycle.MutableLiveData
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef

interface IArticleRepository {
    var refList: MutableLiveData<List<OldItemRef>>

    var articleList: MutableLiveData<List<ArticleItem>>

    suspend fun loadData()

    suspend fun loadData(urlSubPath: String)
}