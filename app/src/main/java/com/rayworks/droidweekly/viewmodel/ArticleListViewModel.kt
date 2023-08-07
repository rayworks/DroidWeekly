package com.rayworks.droidweekly.viewmodel

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.repository.IArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/** * The ViewModel for a list of articles.  */
@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: IArticleRepository,
) : ViewModel() {

    val keyMenuId = "menu_item_id"

    @JvmField
    val dataLoading = MutableStateFlow(true) // ObservableBoolean(false)
    var articleItems = repository.articleList.asLiveData()

    val itemRefs = repository.refList

    // migrate to flow
    val articleState = repository.articleList

    @JvmField
    val articleLoaded = ObservableBoolean(true)

    var selectedRefPath = MutableStateFlow("")

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
                return
            }
        }

        viewModelScope.launch {
            try {
                dataLoading.emit(true)
                repository.loadData()
                dataLoading.emit(false)
            } catch (ex: IOException) {
                onLoadError(ex.message!!)
            }
        }
    }

    /***
     * Loads the [articleItems] by related issue id.
     */
    fun loadBy(issueId: String) {
        viewModelScope.launch {
            try {
                dataLoading.emit(true)
                repository.loadData(issueId)
                dataLoading.emit(false)
            } catch (ex: IOException) {
                onLoadError(ex.message!!)
            }
        }
    }

    private fun onLoadError(err: String) {
        println(err)

        viewModelScope.launch {
            dataLoading.emit(false)
        }

        articleLoaded.set(false)
    }

    suspend fun searchArticles(keyword: String): List<ArticleItem> {
        return repository.loadLocalArticlesBy(keyword)
    }
}
