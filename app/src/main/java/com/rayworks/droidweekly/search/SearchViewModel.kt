package com.rayworks.droidweekly.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.repository.IArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: IArticleRepository
) : ViewModel() {

    var queryFlow: StateFlow<String>? = null
        set(value) {
            field = value
            if (value != null) {
                viewModelScope.launch {
                    setupFlow(onResetData = {
                        items.postValue(Collections.emptyList())
                    })
                }

            }
        }

    private val items: MutableLiveData<List<ArticleItem>> = MutableLiveData()
    val itemsLiveData: LiveData<List<ArticleItem>> = items

    private suspend fun setupFlow(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        onResetData: () -> Unit
    ) {
        if (queryFlow == null) return
        queryFlow?.let { query ->
            query.debounce(300)
                .filter { s: String ->
                    val isEmpty = s.isEmpty()
                    if (isEmpty) {
                        onResetData.invoke()
                    }
                    !isEmpty
                }
                .distinctUntilChanged()
                .flatMapLatest {
                    flowOf(repository.loadLocalArticlesBy(it)).catch {
                        emitAll(flowOf(Collections.emptyList()))
                    }
                }
                .onStart { println(">>> query onStart") }
                .onCompletion { println(">>> query onCompletion") }
                .flowOn(dispatcher)
                .collect {
                    items.value = it
                }
        }

    }
}