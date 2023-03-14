package com.rayworks.droidweekly.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rayworks.droidweekly.dashboard.ui.theme.LightBlue
import com.rayworks.droidweekly.di.KeyValueStorage
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.ui.theme.DroidWeeklyTheme
import com.rayworks.droidweekly.viewmodel.ArticleListViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NewsFeedActivity : ComponentActivity() {
    private val viewModel: ArticleListViewModel by viewModels()

    @JvmField
    @Inject
    var keyValueStorage: KeyValueStorage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            androidx.compose.material.Text(
                                "DroidWeekly",
                                color = Color.White
                            )
                        },
                        backgroundColor = LightBlue
                    )
                },
                content = {
                    feedsList(Modifier.padding(it), vm = viewModel, onViewUrl = { url, title ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(url)
                        DetailActivity.start(this@NewsFeedActivity, url, title = title)
                    })
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.load(true)
    }
}

@Composable
fun feedsList(
    modifier: Modifier = Modifier,
    vm: ArticleListViewModel,
    onViewUrl: (url: String, title:String?) -> Unit
) {

    DroidWeeklyTheme {
        val listState = vm.articleState.collectAsState()

        Surface(color = MaterialTheme.colorScheme.background) {
            if (listState.value != null && listState.value.size > 1)
                LazyColumn(
                    contentPadding = PaddingValues(all = 16.dp)
                ) {
                    items(items = listState.value) {
                        ArticleCard(data = it) { data: ArticleItem ->
                            println("item '${data.title}' clicked")

                            if (data.linkage.isNotEmpty()) {
                                onViewUrl.invoke(data.linkage, data.title)
                            }
                        }
                    }
                }
            else
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Empty list...")
                }
        }
    }
}

@Composable
fun ArticleCard(data: ArticleItem, onItemClick: (data: ArticleItem) -> Unit) {
    Box(modifier = Modifier
        .clickable {
            if (data.description.isNotEmpty())
                onItemClick.invoke(data)
        }) {

        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        ) {
            val invalid = data.imageUrl?.isEmpty() ?: true
            if (!invalid)
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .padding(end = 16.dp)
                )
            Column {
                if (data.title.isNotEmpty())
                    Text(
                        text = data.title,
                        fontSize = 16.sp,
                        color = if (data.description.isEmpty()) Color.White else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .background(
                                color = if (data.description.isEmpty()) LightBlue else Color.Transparent
                            )
                    )

                if (data.description.isNotEmpty())
                    Text(
                        text = data.description,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                    )
            }
        }
    }
}