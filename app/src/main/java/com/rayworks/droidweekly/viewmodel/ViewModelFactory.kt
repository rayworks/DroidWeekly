package com.rayworks.droidweekly.viewmodel

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.rayworks.droidweekly.repository.ArticleRepository

class ViewModelFactory(owner: SavedStateRegistryOwner,
                       defaultState: Bundle?,
                       private val repository: ArticleRepository)
    : AbstractSavedStateViewModelFactory(owner, defaultState) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        return ArticleListViewModel(handle, repository) as T
    }

}