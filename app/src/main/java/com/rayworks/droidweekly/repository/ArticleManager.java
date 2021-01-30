package com.rayworks.droidweekly.repository;

import androidx.annotation.NonNull;

import com.rayworks.droidweekly.model.ArticleItem;
import com.rayworks.droidweekly.model.OldItemRef;
import com.rayworks.droidweekly.repository.database.ArticleDao;
import com.rayworks.droidweekly.repository.database.entity.Article;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@Singleton
public final class ArticleManager {
    private ArticleDao articleDao;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    public ArticleManager(ArticleDao dao) {
        this.articleDao = dao;
    }

    public void search(String key, WeakReference<ArticleDataListener> listener) {
        if(compositeDisposable.isDisposed()) {
            compositeDisposable = new CompositeDisposable();
        }

        Disposable disposable =
                Observable.just("%" + key + "%") // for the fuzzy search
                        .map(str -> articleDao.getArticleByKeyword(str))
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
}
