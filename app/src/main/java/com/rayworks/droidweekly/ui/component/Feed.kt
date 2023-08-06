package com.rayworks.droidweekly.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rayworks.droidweekly.dashboard.ui.theme.LightBlue
import com.rayworks.droidweekly.model.ArticleItem

@Composable
fun FeedList(
    modifier: Modifier = Modifier,
    showLoading: Boolean,
    listState: List<ArticleItem>,
    onViewUrl: (url: String, title: String?) -> Unit,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        if (showLoading) {
            return@Surface Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                )
            }
        }

        if (listState.size > 1) {
            println(">>> list state : $listState")
            LazyColumn(
                modifier,
                contentPadding = PaddingValues(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(items = listState) { index, it ->
                    ArticleCard(data = it) { data: ArticleItem ->
                        println("item '${data.title}' clicked")

                        if (data.linkage.isNotEmpty()) {
                            onViewUrl.invoke(data.linkage, data.title)
                        }
                    }

                    val hiddenSeparator =
                        it.linkage.isEmpty() || index < (listState.size - 1) && listState[index + 1].linkage.isEmpty()
                    if (!hiddenSeparator) {
                        Divider(color = Color.Black, thickness = Dp.Hairline)
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleCard(data: ArticleItem, onItemClick: (data: ArticleItem) -> Unit) {
    Box(
        modifier = Modifier
            .clickable {
                if (data.description.isNotEmpty()) {
                    onItemClick.invoke(data)
                }
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val invalid = data.imageUrl?.isEmpty() ?: true
            if (!invalid) {
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .padding(end = 16.dp),
                )
            }
            Column {
                if (data.title.isNotEmpty()) {
                    val noDesc = data.description.isEmpty()
                    Box(
                        modifier = Modifier
                            .heightIn(min = if (noDesc) 48.dp else Dp.Unspecified)
                            .background(color = if (noDesc) LightBlue else Color.Transparent),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = data.title,
                            fontSize = 16.sp,
                            color = if (noDesc) Color.White else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .padding(
                                    top = if (noDesc) 0.dp else 16.dp,
                                    start = if (noDesc) 16.dp else 0.dp,
                                )
                                .fillMaxWidth(),
                        )
                    }
                }

                if (data.description.isNotEmpty()) {
                    Text(
                        text = data.description,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    )
                }
            }
        }
    }
}
