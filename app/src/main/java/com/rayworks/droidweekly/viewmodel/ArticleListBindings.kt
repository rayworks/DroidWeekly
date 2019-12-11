package com.rayworks.droidweekly.viewmodel

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rayworks.droidweekly.ArticleAdapter
import com.rayworks.droidweekly.model.ArticleItem

object ArticleListBindings {
    @JvmStatic
    @BindingAdapter("items")
    fun setItems(
        view: RecyclerView,
        items: List<ArticleItem>?
    ) {
        val adapter = view.adapter as ArticleAdapter
        items?.let { adapter.update(it) }
    }
}