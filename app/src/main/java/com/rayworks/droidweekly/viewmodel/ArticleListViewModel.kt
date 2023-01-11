package com.rayworks.droidweekly.viewmodel

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.repository.IArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

/** * The ViewModel for a list of articles.  */
@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: IArticleRepository
) : ViewModel() {

    val keyMenuId = "menu_item_id"

    @JvmField
    val dataLoading = ObservableBoolean(false)

    var articleItems = repository.articleList

    val itemRefs = repository.refList

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
                repository.loadData()
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
                repository.loadData(issueId)

                dataLoading.set(false)
            } catch (ex: IOException) {
                onLoadError(ex.message!!)
            }
        }
    }

    private fun onLoadError(err: String) {
        println(err)
        dataLoading.set(false)
        articleLoaded.set(false)
    }

    suspend fun searchArticles(keyword: String): List<ArticleItem> {
        return repository.loadLocalArticlesBy(keyword)
    }
}
