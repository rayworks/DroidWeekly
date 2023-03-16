package com.rayworks.droidweekly.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rayworks.droidweekly.R
import com.rayworks.droidweekly.dashboard.ui.theme.LightBlue
import com.rayworks.droidweekly.di.KeyValueStorage
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef
import com.rayworks.droidweekly.ui.theme.DroidWeeklyTheme
import com.rayworks.droidweekly.viewmodel.ArticleListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
            BuildContent()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BuildContent() {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val scaffoldState = rememberScaffoldState()

        val coroutineScope = rememberCoroutineScope()

        DroidWeeklyTheme {
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = {
                    FeedTopAppBar(
                        openDrawer = {
                            coroutineScope.launch {
                                scaffoldState.drawerState.open()
                            }
                        },
                        appBarState = topAppBarState,
                        scrollBehavior = scrollBehavior
                    )
                },
                content = { innerPadding ->
                    val contentModifier = Modifier
                        .padding(innerPadding)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)

                    FeedList(contentModifier, vm = viewModel, onViewUrl = { url, title ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(url)
                        DetailActivity.start(this@NewsFeedActivity, url, title = title)
                    })
                },
                drawerContent = {
                    BuildDrawerContent(vm = viewModel) { ref ->
                        Toast.makeText(
                            this@NewsFeedActivity,
                            "${ref.title} clicked",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }

    @Composable
    private fun BuildDrawerContent(
        modifier: Modifier = Modifier,
        vm: ArticleListViewModel,
        onRefClick: (ref: OldItemRef) -> Unit
    ) {
        val refState = vm.itemRefState.collectAsState()

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            if (refState.value.isNotEmpty()) {
                items(refState.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onRefClick.invoke(it) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            it.title,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = Color.Blue
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.load(true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTopAppBar(
    openDrawer: () -> Unit, modifier: Modifier = Modifier,
    appBarState: TopAppBarState = rememberTopAppBarState(),
    scrollBehavior: TopAppBarScrollBehavior? = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        appBarState
    )
) {
    val ctx = LocalContext.current
    CenterAlignedTopAppBar(
        title = {
            androidx.compose.material.Text(
                "DroidWeekly",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    Icons.Default.Menu,
                    "menu",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = {
                Toast.makeText(
                    ctx,
                    "Search is not yet implemented ;)",
                    Toast.LENGTH_LONG
                ).show()
            }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.cd_search),
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LightBlue),
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}

@Composable
fun FeedList(
    modifier: Modifier = Modifier,
    vm: ArticleListViewModel,
    onViewUrl: (url: String, title: String?) -> Unit
) {

    val listState = vm.articleState.collectAsState()

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        if (listState.value != null && listState.value.size > 1)
            LazyColumn(
                modifier,
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
        else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
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