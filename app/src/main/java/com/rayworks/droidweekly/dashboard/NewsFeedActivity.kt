package com.rayworks.droidweekly.dashboard

import android.content.Context
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
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
import timber.log.Timber
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
            BuildContent { url, title ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                DetailActivity.start(this@NewsFeedActivity, url, title = title)
            }

            loadInitData()
        }
    }

    private fun loadInitData() {
        val hasLastRef = viewModel.selectedRefPath.value.isNotEmpty()
        Timber.d(">>> last selected : $hasLastRef")
        if (viewModel.selectedRefPath.value.isNotEmpty()) {
            viewModel.loadBy(viewModel.selectedRefPath.value)
        } else {
            viewModel.load(true)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BuildContent(onArticleClick: (url: String, title: String?) -> Unit) {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val scaffoldState = rememberScaffoldState()

        // used for user interaction
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
                        scrollBehavior = scrollBehavior,
                        context = this@NewsFeedActivity,
                    )
                },
                content = { innerPadding ->
                    val contentModifier = Modifier
                        .padding(innerPadding)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)

                    // minimize the data model scope and pass only the necessary data
                    val listState by viewModel.articleState.collectAsState()
                    val showLoading by viewModel.dataLoading.collectAsState()

                    FeedList(
                        contentModifier,
                        showLoading = showLoading,
                        listState = listState,
                        onViewUrl = onArticleClick,
                    )
                },
                drawerContent = {
                    val refState by viewModel.itemRefState.collectAsState()
                    val refSelected by viewModel.selectedRefPath.collectAsState()

                    BuildDrawerContent(itemRefs = refState, refSelectedPath = refSelected) { ref ->
                        viewModel.loadBy(ref.relativePath)

                        // update the selected issue
                        lifecycleScope.launch {
                            viewModel.selectedRefPath.emit(ref.relativePath)
                        }

                        coroutineScope.launch {
                            scaffoldState.drawerState.close()
                        }
                    }
                },
            )
        }
    }

    @Composable
    private fun BuildDrawerContent(
        modifier: Modifier = Modifier,
        itemRefs: List<OldItemRef>,
        refSelectedPath: String,
        onRefClick: (ref: OldItemRef) -> Unit,
    ) {
        if (refSelectedPath.isEmpty() && itemRefs.isNotEmpty()) {
            LaunchedEffect(key1 = itemRefs[0].relativePath) {
                viewModel.selectedRefPath.emit(itemRefs[0].relativePath)
            }
        }

        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (itemRefs.isNotEmpty()) {
                items(itemRefs) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .height(32.dp)
                            .clickable {
                                onRefClick.invoke(it)
                            },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row {
                            Text(
                                it.title,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                color = Color.Blue,
                            )
                            if (refSelectedPath == it.relativePath) {
                                Icon(imageVector = Icons.Default.Check, "", tint = Color.Blue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTopAppBar(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    appBarState: TopAppBarState = rememberTopAppBarState(),
    scrollBehavior: TopAppBarScrollBehavior? = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        appBarState,
    ),
    context: Context,
) {
    val ctx = LocalContext.current
    CenterAlignedTopAppBar(
        title = {
            androidx.compose.material.Text(
                text = context.getString(R.string.app_name),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    Icons.Default.Menu,
                    "menu",
                    tint = Color.White,
                )
            }
        },
        actions = {
            IconButton(onClick = {
                Toast.makeText(
                    ctx,
                    "Search is not yet implemented ;)",
                    Toast.LENGTH_LONG,
                ).show()
            }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.cd_search),
                    tint = Color.White,
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LightBlue),
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )
}

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
