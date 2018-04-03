package com.rayworks.droidweekly.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;

import com.rayworks.droidweekly.data.ArticleManager;
import com.rayworks.droidweekly.model.ArticleItem;

import java.util.List;

/** * The ViewModel for a list of articles. */
public class ArticleListViewModel extends ViewModel implements ArticleManager.ArticleDataListener {
    public final ObservableBoolean dataLoading = new ObservableBoolean(false);
    public final ObservableField<List<ArticleItem>> articles = new ObservableField<>();

    public final ObservableBoolean articleLoaded = new ObservableBoolean(true);

    private final ArticleManager manager;

    public ArticleListViewModel(ArticleManager manager) {
        this.manager = manager;
    }

    public ObservableField<List<ArticleItem>> getArticles() {
        return articles;
    }

    public void load(boolean forced) {
        if (!forced) {
            // check the cache first
            List<ArticleItem> articleItems = articles.get();
            if (articleItems != null && articleItems.size() > 0) {
                onComplete(articleItems);
                return;
            }
        }

        dataLoading.set(true);
        manager.setDataListener(this).loadData();
    }

    public void loadBy(String issueId) {

    }

    @Override
    public void onLoadError(String err) {
        System.out.println(err);

        dataLoading.set(false);
        articleLoaded.set(false);
    }

    @Override
    public void onComplete(List<ArticleItem> items) {
        dataLoading.set(false);
        articles.set(items);
    }

    @Override
    protected void onCleared() {
        manager.setDataListener(null);
        super.onCleared();
    }
}
