package com.rayworks.droidweekly.viewmodel;

import android.databinding.BindingAdapter;
import android.support.v7.widget.RecyclerView;

import com.rayworks.droidweekly.ArticleAdapter;
import com.rayworks.droidweekly.model.ArticleItem;

import java.util.List;

public class ArticleListBindings {
    @SuppressWarnings("unchecked")
    @BindingAdapter("app:items")
    public static void setItems(RecyclerView listView, List<ArticleItem> items) {
        ArticleAdapter adapter = (ArticleAdapter) listView.getAdapter();

        if (adapter != null) {
            adapter.update(items);
        }
    }
}
