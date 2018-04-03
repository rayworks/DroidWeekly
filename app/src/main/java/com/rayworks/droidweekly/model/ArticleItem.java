package com.rayworks.droidweekly.model;

public class ArticleItem {
    public String title;

    public String description;

    public String linkage;

    public String imageUrl;

    public int imgFrameColor;

    public ArticleItem(String title, String description, String linkage) {
        this.title = title;
        this.description = description;
        this.linkage = linkage;
    }

    public ArticleItem() {}

    @Override
    public String toString() {
        return "ArticleItem{"
                + "title='"
                + title
                + '\''
                + ", description='"
                + description
                + '\''
                + ", linkage='"
                + linkage
                + '\''
                + ", imageUrl='"
                + imageUrl
                + '\''
                + ", imgFrameColor="
                + imgFrameColor
                + '}';
    }
}
