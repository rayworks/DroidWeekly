package com.rayworks.droidweekly.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.rayworks.droidweekly.App;
import com.rayworks.droidweekly.model.ArticleItem;
import com.rayworks.droidweekly.model.OldItemRef;
import com.rayworks.droidweekly.repository.database.IssueDatabase;
import com.rayworks.droidweekly.repository.database.IssueDatabaseKt;
import com.rayworks.droidweekly.repository.database.entity.Article;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public final class ArticleManager {
    // To be injected
    private IssueDatabase database;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private ArticleManager() {
        Context context = App.getApp().getApplicationContext();
        initStorage(context);
    }

    public static ArticleManager getInstance() {
        return ManagerHolder.articleManager;
    }

    private void initStorage(Context context) {
        // create database
        database = IssueDatabaseKt.getDatabase(context);
    }

    public void search(String key, WeakReference<ArticleDataListener> listener) {
        Disposable disposable =
                Observable.just("%" + key + "%") // for the fuzzy search
                        .map(str -> database.articleDao().getArticleByKeyword(str))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .singleOrError()
                        .subscribe(
                                list -> {
                                    System.out.println(">>><<< matched item titles : ");
                                    for (Article article : list) {
                                        System.out.println(">>> article : " + article.getTitle());
                                    }
                                    System.out.println(">>><<< matched item titles");

                                    if (listener.get() != null) {
                                        listener.get().onComplete(getArticleModels(list));
                                    }
                                },
                                throwable -> {
                                    throwable.printStackTrace();
                                    if (listener.get() != null) {
                                        listener.get().onLoadError(throwable.getMessage());
                                    }
                                });

        compositeDisposable.add(disposable);
    }

    public void dispose() {
        compositeDisposable.dispose();
    }

    @NonNull
    private List<ArticleItem> getArticleModels(List<Article> articles) {
        List<ArticleItem> items = new LinkedList<>();
        for (Article article : articles) {
            ArticleItem articleItem =
                    new ArticleItem(
                            article.getTitle(), article.getDescription(), article.getLinkage());

            articleItem.setImgFrameColor(article.getImgFrameColor());
            articleItem.setImageUrl(article.getImageUrl());

            items.add(articleItem);
        }
        return items;
    }

    public interface ArticleDataListener {
        void onLoadError(String err);

        void onOldRefItemsLoaded(List<OldItemRef> itemRefs);

        void onComplete(List<ArticleItem> items);
    }

    private static class ManagerHolder {
        private static ArticleManager articleManager = new ArticleManager();
    }
}
