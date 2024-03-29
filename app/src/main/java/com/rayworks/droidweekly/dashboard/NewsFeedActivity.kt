package com.rayworks.droidweekly.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.rayworks.droidweekly.R
import com.rayworks.droidweekly.di.KeyValueStorage
import com.rayworks.droidweekly.model.OldItemRef
import com.rayworks.droidweekly.search.SearchComposeActivity
import com.rayworks.droidweekly.ui.component.FeedList
import com.rayworks.droidweekly.ui.theme.DroidWeeklyTheme
import com.rayworks.droidweekly.ui.theme.LightBlue
import com.rayworks.droidweekly.viewmodel.ArticleListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/***
 * The latest dash board
 */
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
        val selectedPath = viewModel.selectedPathStateFlow.value
        val hasLastRef = selectedPath.isNotEmpty()
        Timber.d(">>> last selected : $hasLastRef, url path : $selectedPath")
        if (selectedPath.isNotEmpty()) {
            viewModel.loadBy(selectedPath)
        } else {
            viewModel.load(true)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun BuildContent(onArticleClick: (url: String, title: String?) -> Unit) {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

        // used for user interaction
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val coroutineScope = rememberCoroutineScope()

        DroidWeeklyTheme {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    val refState by viewModel.itemRefs.collectAsState()
                    val refSelected by viewModel.selectedPathStateFlow.collectAsState()

                    BuildDrawerContent(
                        itemRefs = refState,
                        refSelectedPath = refSelected
                    ) { ref ->
                        viewModel.loadBy(ref.relativePath)

                        // update the selected issue
                        lifecycleScope.launch {
                            viewModel.selectIssuePath(ref.relativePath)
                        }

                        coroutineScope.launch {
                            drawerState.close()
                        }
                    }
                }, content = {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            FeedTopAppBar(
                                openDrawer = {
                                    coroutineScope.launch {
                                        drawerState.open()
                                    }
                                },
                                appBarState = topAppBarState,
                                scrollBehavior = scrollBehavior,
                                context = this@NewsFeedActivity,
                                onSearch = { SearchComposeActivity.start(this@NewsFeedActivity) },
                            )
                        })
                    {
                        // minimize the data model scope and pass only the necessary data
                        val listState by viewModel.articleState.collectAsState()
                        val showLoading by viewModel.dataLoading.collectAsState()

                        FeedList(
                            Modifier.padding(it),
                            showLoading = showLoading,
                            listState = listState,
                            onViewUrl = onArticleClick,
                        )
                    }
                })
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
            val path = itemRefs[0].relativePath
            LaunchedEffect(key1 = path) {
                viewModel.selectIssuePath(path)
            }
        }

        ModalDrawerSheet {
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
}

/***
 * The custom toolbar for news list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedTopAppBar(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    appBarState: TopAppBarState = rememberTopAppBarState(),
    scrollBehavior: TopAppBarScrollBehavior? = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        appBarState,
    ),
    context: Context,
    onSearch: () -> Unit,
) {
    val ctx = LocalContext.current
    CenterAlignedTopAppBar(
        title = {
            Text(
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
                onSearch.invoke()
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
