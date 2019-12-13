package com.rayworks.droidweekly.repository.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/***
 * The Article Entity data
 */
@Entity
class Article(
        @PrimaryKey(autoGenerate = true) val uid: Int = 0, var title: String,
        @ColumnInfo var description: String,
        @ColumnInfo var linkage: String,
        @ColumnInfo(name = "image_url") var imageUrl: String,
        @ColumnInfo(name = "img_frame_color") var imgFrameColor: Int = 0,
        @ColumnInfo(name = "issue_id") var issueId: Int = 0,
        @ColumnInfo var order: Int = 0
)