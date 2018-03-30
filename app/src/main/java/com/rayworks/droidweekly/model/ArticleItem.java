package com.rayworks.droidweekly.model;

import com.github.florent37.retrojsoup.annotations.JsoupHref;
import com.github.florent37.retrojsoup.annotations.JsoupText;

public class ArticleItem {
    @JsoupText(".tr(0):td(0) a")
    public String title;

    @JsoupText(".tr(0):td(0) p")
    public String description;

    @JsoupHref(".tr(0):td(0) a")
    public String linkage;

    public ArticleItem(String title, String description, String linkage) {
        this.title = title;
        this.description = description;
        this.linkage = linkage;
    }

    public ArticleItem() {
    }

    @Override
    public String toString() {
        return "ArticleItem{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", linkage='" + linkage + '\'' +
                '}';
    }
}
