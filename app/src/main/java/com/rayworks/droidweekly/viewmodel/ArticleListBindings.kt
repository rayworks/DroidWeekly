package com.rayworks.droidweekly.viewmodel

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rayworks.droidweekly.ui.component.ArticleAdapter
import com.rayworks.droidweekly.model.ArticleItem

/***
 * The custom binding for updating the data of a [RecyclerView.Adapter].
 */
object ArticleListBindings {
    @JvmStatic
    @BindingAdapter("app:items")
    fun setItems(
        listView: RecyclerView,
        items: List<ArticleItem>?
    ) {
        val adapter = listView.adapter as ArticleAdapter?
        adapter?.update(items)
    }
}
