package com.rayworks.droidweekly.model;

import com.github.florent37.retrojsoup.annotations.Select;

import io.reactivex.Observable;

public interface ArticleService {
    @Select("#issue .table")
    Observable<ArticleItem> articles();
}
