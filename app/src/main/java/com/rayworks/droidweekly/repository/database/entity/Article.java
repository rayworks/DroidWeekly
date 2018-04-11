package com.rayworks.droidweekly.repository.database.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Article {
    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo private String title;

    @ColumnInfo private String description;

    @ColumnInfo private String linkage;

    @ColumnInfo(name = "image_url")
    private String imageUrl;

    @ColumnInfo(name = "img_frame_color")
    private int imgFrameColor;

    @ColumnInfo(name = "issue_id")
    private int issueId;

    @ColumnInfo private int order;

    public int getIssueId() {
        return issueId;
    }

    public void setIssueId(int issueId) {
        this.issueId = issueId;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLinkage() {
        return linkage;
    }

    public void setLinkage(String linkage) {
        this.linkage = linkage;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getImgFrameColor() {
        return imgFrameColor;
    }

    public void setImgFrameColor(int imgFrameColor) {
        this.imgFrameColor = imgFrameColor;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
