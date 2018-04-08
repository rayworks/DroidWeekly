package com.rayworks.droidweekly.model;

public class OldItemRef {
    private final String title;
    private final String relativePath;

    public OldItemRef(String title, String relativePath) {
        this.title = title;
        this.relativePath = relativePath;
    }

    public String getTitle() {
        return title;
    }

    public String getRelativePath() {
        return relativePath;
    }

    @Override
    public String toString() {
        return "OldItemRef{"
                + "title='"
                + title
                + '\''
                + ", relativePath='"
                + relativePath
                + '\''
                + '}';
    }
}
