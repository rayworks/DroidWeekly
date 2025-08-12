package com.rayworks.droidweekly.search

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rayworks.droidweekly.dashboard.DetailActivity
import com.rayworks.droidweekly.ui.component.FeedList
import com.rayworks.droidweekly.ui.theme.DroidWeeklyTheme
import com.rayworks.droidweekly.ui.theme.LightBlue
import dagger.hilt.android.AndroidEntryPoint

/***
 * The search page
 */
@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class SearchComposeActivity : ComponentActivity() {
    private val searchViewModel: SearchViewModel by viewModels()

    companion object {
        /***
         * A convenient way to navigate to the search page
         */
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, SearchComposeActivity::class.java)
            context.startActivity(starter)
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DroidWeeklyTheme {
                Scaffold(
                    modifier = Modifier,
                    topBar = { BuildTopBar { this@SearchComposeActivity.finish() } },
                ) {
                    BuildBody(Modifier.padding(it)) { url, title ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(url)
                        DetailActivity.start(this@SearchComposeActivity, url, title = title)
                    }
                }
            }
        }
    }

    @Composable
    private fun BuildBody(
        modifier: Modifier,
        onArticleClick: (url: String, title: String?) -> Unit,
    ) {
        val itemsFlow by searchViewModel.itemsFlow.collectAsState()

        FeedList(
            modifier = modifier,
            showLoading = false,
            listState = itemsFlow,
            onViewUrl = onArticleClick,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BuildTopBar(modifier: Modifier = Modifier, onUpClick: () -> Unit) {
        val input by searchViewModel.query.collectAsState()

        CenterAlignedTopAppBar(
            title = {
                BuildEditor(
                    "Search",
                    input = input,
                    onSearch = { str ->
                        searchViewModel.setQuery(str)
                    },
                    onClear = {
                        searchViewModel.setQuery("")
                    },
                )
            },

            modifier = modifier
                .background(LightBlue)
                .statusBarsPadding(),

            navigationIcon = {
                IconButton(onClick = onUpClick) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            },
        )
    }

    @Composable
    private fun BuildEditor(
        hint: String,
        input: String,
        onSearch: (String) -> Unit,
        onClear: () -> Unit,
    ) {
        TextField(
            value = input,
            onValueChange = { newValue -> onSearch(newValue) },
            label = { Text(hint) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { onClear.invoke() }) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "")
                }
            },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
    }
}
