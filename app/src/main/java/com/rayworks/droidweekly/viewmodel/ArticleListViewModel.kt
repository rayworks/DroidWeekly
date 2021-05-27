package com.rayworks.droidweekly.viewmodel

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef
import com.rayworks.droidweekly.repository.ArticleManager.ArticleDataListener
import com.rayworks.droidweekly.repository.IArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/** * The ViewModel for a list of articles.  */
@HiltViewModel
class ArticleListViewModel @Inject constructor (
    private val savedStateHandle: SavedStateHandle,
    private val manager: IArticleRepository
) : ViewModel(),
        ArticleDataListener {

    val keyMenuId = "menu_item_id"

    @JvmField
    val dataLoading = ObservableBoolean(false)

    // TODO: binding not working ?!
    // var articles: ObservableField<List<ArticleItem>> = ObservableField()

    var articleItems = manager.articleList

    val itemRefs = manager.refList

    @JvmField
    val articleLoaded = ObservableBoolean(true)

    var selectedItemId: Int = 0
        set(value) {
            field = value

            savedStateHandle.set(keyMenuId, selectedItemId)
            Timber.i(">>> Item position saved : $value")
        }
        get() {
            if (field == 0) {
                if (savedStateHandle.contains(keyMenuId)) {
                    field = savedStateHandle.get(keyMenuId) ?: 0
                    Timber.i(">>> Item position restored : $field")
                }
            }
            return field
        }

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
            } catch (ex: IOException) {
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
            } catch (ex: IOException) {
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
        // this.itemRefs.set(itemRefs)
    }

    override fun onComplete(items: List<ArticleItem>) {
        dataLoading.set(false)
        // articles.value = (items)
    }

    override fun onCleared() {
        // manager.setDataListener(null)
        super.onCleared()
    }
}
