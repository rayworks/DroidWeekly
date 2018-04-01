package com.rayworks.droidweekly.model;

import io.reactivex.Observable;

public interface ArticleService {
    Observable<ArticleItem> articles();
}
