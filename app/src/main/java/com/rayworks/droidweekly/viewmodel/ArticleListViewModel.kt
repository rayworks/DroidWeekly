package com.rayworks.droidweekly.viewmodel

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef
import com.rayworks.droidweekly.repository.ArticleManager.ArticleDataListener
import com.rayworks.droidweekly.repository.ArticleRepository
import kotlinx.coroutines.launch

/** * The ViewModel for a list of articles.  */
class ArticleListViewModel(private val manager: ArticleRepository) : ViewModel(),
        ArticleDataListener {

    @JvmField
    val dataLoading = ObservableBoolean(false)

    // TODO: binding not working ?!
    // var articles: ObservableField<List<ArticleItem>> = ObservableField()

    var articleItems = manager.articleList

    val itemRefs = manager.refList

    @JvmField
    val articleLoaded = ObservableBoolean(true)

    /***
     * Loads the [articleItems] for latest issue.
     */
    fun load(forced: Boolean) {
        if (!forced) { // check the cache first
            if (!articleItems.value.isNullOrEmpty()) {
                articleItems.postValue(articleItems.value)

                return
            }
        }

        dataLoading.set(true)
        viewModelScope.launch {
            try {
                manager.loadData()
                dataLoading.set(false)
            } catch (ex: Exception) {
                onLoadError(ex.message!!)
            }
        }
    }

    /***
     * Loads the [articleItems] by related issue id.
     */
    fun loadBy(issueId: String) {
        dataLoading.set(true)

        viewModelScope.launch {
            try {
                manager.loadData(issueId)

                dataLoading.set(false)
            } catch (ex: Exception) {
                onLoadError(ex.message!!)
            }
        }
    }

    override fun onLoadError(err: String) {
        println(err)
        dataLoading.set(false)
        articleLoaded.set(false)
    }

    override fun onOldRefItemsLoaded(itemRefs: List<OldItemRef>) {
        //this.itemRefs.set(itemRefs)
    }

    override fun onComplete(items: List<ArticleItem>) {
        dataLoading.set(false)
        // articles.value = (items)
    }

    override fun onCleared() {
        //manager.setDataListener(null)
        super.onCleared()
    }
}